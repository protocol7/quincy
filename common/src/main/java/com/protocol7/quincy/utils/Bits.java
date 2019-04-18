package com.protocol7.quincy.utils;

public class Bits {

  public static long set(final long v, final int bit) {
    return v | 1 << bit;
  }

  public static int set(final int v, final int bit) {
    return v | 1 << bit;
  }

  public static long unset(final long v, final int bit) {
    return v & ~(1 << bit);
  }

  public static int unset(final int v, final int bit) {
    return v & ~(1 << bit);
  }
}
