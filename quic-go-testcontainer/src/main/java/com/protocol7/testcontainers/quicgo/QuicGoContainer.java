package com.protocol7.testcontainers.quicgo;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import java.net.InetSocketAddress;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class QuicGoContainer extends GenericContainer {

  private static ImageFromDockerfile image() {
    return new ImageFromDockerfile("quic-go", false)
        .withFileFromClasspath("Dockerfile", "QuicGoDockerfile")
        .withFileFromClasspath("main.go", "main.go");
  }

  public QuicGoContainer() {
    super(image());

    withExposedPorts(6121);
    waitingFor(Wait.forLogMessage(".*server Listening for udp connections on.*\\n", 1));
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
}
