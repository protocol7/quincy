package com.protocol7.nettyquic.utils;

public class Bits {

  public static long set(long v, int bit) {
    return v | 1 << bit;
  }

  public static long unset(long v, int bit) {
    return v & ~(1 << bit);
  }
}
