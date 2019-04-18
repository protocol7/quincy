package com.protocol7.testcontainers.quicly;

import org.testcontainers.images.builder.ImageFromDockerfile;

public class QuiclyClientContainer extends QuiclyContainer {

  private static ImageFromDockerfile image() {
    return new ImageFromDockerfile("quicly-client", false)
        .withFileFromClasspath("Dockerfile", "QuiclyClientDockerfile");
  }

  public QuiclyClientContainer(final String host, final int port) {
    super(image());

    // withCommand("./cli", "-vv", "-x", "X25519", host, Integer.toString(port));
  }
}
