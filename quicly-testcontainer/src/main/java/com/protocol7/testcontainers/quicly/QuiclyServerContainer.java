package com.protocol7.testcontainers.quicly;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class QuiclyServerContainer extends QuiclyContainer {

  private static ImageFromDockerfile image() {
    return new ImageFromDockerfile("quicly", false)
        .withFileFromClasspath("Dockerfile", "QuiclyDockerfile");
  }

  public QuiclyServerContainer() {
    super(image());

    withExposedPorts(4433);
    waitingFor(Wait.forHealthcheck());
  }
}
