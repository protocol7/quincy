package com.protocol7.nettyquick.utils;

import com.google.common.io.BaseEncoding;

public class Hex {

  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

  public static byte[] dehex(String s) {
    return HEX.decode(s.replace(" ", ""));
  }
}
