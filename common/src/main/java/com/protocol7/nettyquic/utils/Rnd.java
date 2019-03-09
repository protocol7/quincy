package com.protocol7.nettyquic.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Rnd {

  private static final BigInteger TWO = BigInteger.valueOf(2);
  private static SecureRandom rnd = new SecureRandom();

  public static void rndBytes(byte[] b) {
    rnd.nextBytes(b);
  }

  public static byte[] rndBytes(int length) {
    byte[] b = new byte[length];
    rndBytes(b);
    return b;
  }

  public static int rndInt(int min, int max) {
    return rnd.nextInt(max - min) + min;
  }

  public static int rndInt() {
    return rnd.nextInt();
  }
}
