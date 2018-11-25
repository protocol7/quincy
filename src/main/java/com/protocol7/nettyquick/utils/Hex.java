package com.protocol7.nettyquick.utils;

import com.google.common.io.BaseEncoding;

public class Hex {

  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

  public static String hex(byte[] b) {
    return HEX.encode(b);
  }

  public static String hex(byte b) {
    return HEX.encode(new byte[] {b});
  }

  public static byte[] dehex(String s) {
    return HEX.decode(s.replace(" ", "").toLowerCase());
  }
}
