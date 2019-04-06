package com.protocol7.testcontainers.quicly;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class QuiclyContainer extends GenericContainer {

  private final Logger log = LoggerFactory.getLogger("quicly");

  private final List<String> logStatements = new ArrayList<>();

  public QuiclyContainer(ImageFromDockerfile image) {
    super(image);
  }

  @Override
  protected void containerIsStarted(InspectContainerResponse containerInfo) {
    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
    followOutput(logConsumer);
    followOutput(
        new Consumer<OutputFrame>() {
          @Override
          public void accept(final OutputFrame outputFrame) {
            logStatements.add(outputFrame.getUtf8String());
          }
        });
  }

  private int getUdpPort() {
    return Integer.valueOf(
        getContainerInfo()
            .getNetworkSettings()
            .getPorts()
            .getBindings()
            .get(new ExposedPort(4433, InternetProtocol.UDP))[0]
            .getHostPortSpec());
  }

  public InetSocketAddress getAddress() {
    return new InetSocketAddress(getContainerIpAddress(), getUdpPort());
  }

  public List<QuiclyPacket> getPackets() {
    return PacketParser.parse(logStatements);
  }
}
