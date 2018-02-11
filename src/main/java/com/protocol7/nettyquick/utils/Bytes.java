package com.protocol7.nettyquick.utils;

public class Bytes {

  public static String binary(int i) {
    return Integer.toBinaryString(i);
  }

  public static byte[] concat(byte[]... bs) {
    return com.google.common.primitives.Bytes.concat(bs);
  }
}
