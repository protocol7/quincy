package com.protocol7.nettyquick.utils;

import io.netty.buffer.ByteBuf;

public class Bytes {

  public static byte[] asArray(ByteBuf bb) {
    byte[] b = new byte[bb.readableBytes()];
    bb.readBytes(b);
    return b;
  }


  public static String binary(int i) {
    return Integer.toBinaryString(i);
  }

  public static byte[] concat(byte[]... bs) {
    return com.google.common.primitives.Bytes.concat(bs);
  }

  public static void debug(ByteBuf bb) {
    debug("", bb);
  }

  public static void debug(String msg, ByteBuf bb) {
    int index = bb.readerIndex();
    byte[] b = new byte[bb.readableBytes()];
    bb.readBytes(b);
    bb.readerIndex(index);

    System.out.println(msg + Hex.hex(b));
  }


}
