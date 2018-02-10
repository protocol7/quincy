package com.protocol7.nettyquick.utils;

import java.util.concurrent.ThreadLocalRandom;

public class Rnd {

  public static long rndLong() {
    return ThreadLocalRandom.current().nextLong();
  }

}
