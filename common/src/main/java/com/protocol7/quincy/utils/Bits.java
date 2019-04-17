package com.protocol7.quincy.utils;

public class Bits {

  public static long set(long v, int bit) {
    return v | 1 << bit;
  }

  public static int set(int v, int bit) {
    return v | 1 << bit;
  }

  public static long unset(long v, int bit) {
    return v & ~(1 << bit);
  }

  public static int unset(int v, int bit) {
    return v & ~(1 << bit);
  }
}
