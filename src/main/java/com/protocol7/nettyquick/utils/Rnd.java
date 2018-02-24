package com.protocol7.nettyquick.utils;

import java.util.concurrent.ThreadLocalRandom;

public class Rnd {

  public static long rndLong() {
    return ThreadLocalRandom.current().nextLong();
  }

  public static long rndLong(long min, long max) {
    return ThreadLocalRandom.current().nextLong(min, max);
  }

}
