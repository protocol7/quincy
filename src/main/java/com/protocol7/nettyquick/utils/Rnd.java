package com.protocol7.nettyquick.utils;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.primitives.UnsignedLong;

public class Rnd {

  private static final BigInteger TWO = BigInteger.valueOf(2);

  public static long rndLong() {
    return ThreadLocalRandom.current().nextLong();
  }

  public static void rndBytes(byte[] b) {
    ThreadLocalRandom.current().nextBytes(b);
  }

  public static long rndLong(long min, long max) {
    return ThreadLocalRandom.current().nextLong(min, max);
  }

  public static UnsignedLong rndUnsignedLong() {
    byte[] b = new byte[8];
    ThreadLocalRandom.current().nextBytes(b);
    return UnsignedLong.valueOf(new BigInteger(b).abs().multiply(TWO));
  }

}
