package com.protocol7.testcontainers.quicgo;

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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class QuicGoContainer extends GenericContainer {

  private final Logger log = LoggerFactory.getLogger("quic-go");

  private static ImageFromDockerfile image() {
    return new ImageFromDockerfile("quic-go", false)
        .withFileFromClasspath("Dockerfile", "QuicGoDockerfile");
  }

  private final List<String> logStatements = new ArrayList<>();

  public QuicGoContainer() {
    super(image());

    withExposedPorts(6121);
    waitingFor(Wait.forLogMessage(".*server Listening for udp connections on.*\\n", 1));
  }

  @Override
  protected void containerIsStarted(final InspectContainerResponse containerInfo) {
    final Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
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
            .get(new ExposedPort(6121, InternetProtocol.UDP))[0]
            .getHostPortSpec());
  }

  public InetSocketAddress getAddress() {
    return new InetSocketAddress(getContainerIpAddress(), getUdpPort());
  }

  public List<QuicGoPacket> getPackets() {
    return QuicGoPacketParser.parse(logStatements);
  }
}
