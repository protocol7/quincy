package com.protocol7.quincy.it;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import java.io.File;
import java.net.InetSocketAddress;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class QuicheContainer extends GenericContainer {

  public QuicheContainer(final boolean wait) {
    super(
        new ImageFromDockerfile("quiche", false)
            .withFileFromClasspath("Dockerfile", "QuicheDockerfile"));

    if (wait) {
      waitingFor(Wait.forLogMessage(".*listening on.*", 1));
    } else {
      withStartupCheckStrategy(new OneShotStartupCheckStrategy());
    }
    withFileSystemBind(
        new File("src/test/resources").getAbsolutePath(), "/keys", BindMode.READ_ONLY);
  }

  private int getUdpPort() {
    return Integer.parseInt(
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
}
