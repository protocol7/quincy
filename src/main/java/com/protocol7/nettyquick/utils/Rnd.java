package com.protocol7.nettyquick.utils;

import com.google.common.primitives.UnsignedLong;

import java.math.BigInteger;
import java.security.SecureRandom;

public class Rnd {

  private static final BigInteger TWO = BigInteger.valueOf(2);
  private static SecureRandom rnd = new SecureRandom();

  public static long rndLong() {
    return rnd.nextLong();
  }

  public static void rndBytes(byte[] b) {
    rnd.nextBytes(b);
  }

  public static byte[] rndBytes(int length) {
    byte[] b = new byte[length];
    rndBytes(b);
    return b;
  }

  public static int rndInt(int min, int  max) {
    return rnd.nextInt(max - min) + min;
  }

  public static UnsignedLong rndUnsignedLong() {
    byte[] b = new byte[8];
    rnd.nextBytes(b);
    return UnsignedLong.valueOf(new BigInteger(b).abs().multiply(TWO));
  }

  public static int rndInt() {
    return rnd.nextInt();
  }
}
