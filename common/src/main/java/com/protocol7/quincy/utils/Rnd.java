package com.protocol7.quincy.utils;

import java.security.SecureRandom;

public class Rnd {

  private static final SecureRandom rnd = new SecureRandom();

  public static void rndBytes(final byte[] b) {
    rnd.nextBytes(b);
  }

  public static byte[] rndBytes(final int length) {
    final byte[] b = new byte[length];
    rndBytes(b);
    return b;
  }

  public static int rndInt(final int min, final int max) {
    return rnd.nextInt(max - min) + min;
  }

  public static int rndInt() {
    return rnd.nextInt();
  }
}
