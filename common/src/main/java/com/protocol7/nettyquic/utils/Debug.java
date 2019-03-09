package com.protocol7.nettyquic.utils;

import io.netty.buffer.ByteBuf;

public class Debug {

  public static String binary(int i) {
    return Integer.toBinaryString(i);
  }

  public static void buffer(ByteBuf bb) {
    buffer("", bb);
  }

  public static void buffer(String msg, ByteBuf bb) {
    int index = bb.readerIndex();
    byte[] b = new byte[bb.readableBytes()];
    bb.readBytes(b);
    bb.readerIndex(index);

    System.out.println(msg + Hex.hex(b));
  }
}
