package com.protocol7.quincy.utils;

import io.netty.buffer.ByteBuf;

public class Debug {

  public static void buffer(final ByteBuf bb) {
    buffer("", bb);
  }

  public static void buffer(final String msg, final ByteBuf bb) {
    final int index = bb.readerIndex();
    final byte[] b = new byte[bb.readableBytes()];
    bb.readBytes(b);
    bb.readerIndex(index);

    System.out.println(msg + Hex.hex(b));
  }
}
