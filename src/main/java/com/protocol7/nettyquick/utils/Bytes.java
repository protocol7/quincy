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


    public static int read24(ByteBuf bb) {
        byte[] b = new byte[3];
        bb.readBytes(b);
        return (b[0] & 0xFF) << 16 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF);
    }

  public static void write24(ByteBuf bb, int value) {
      bb.writeByte((value >> 16) & 0xFF);
      bb.writeByte((value >> 8)  & 0xFF);
      bb.writeByte(value & 0xFF);
  }

  public static void write24(ByteBuf bb, int value, int position) {
      bb.setByte(position, (value >> 16) & 0xFF);
      bb.setByte(position + 1, (value >> 8)  & 0xFF);
      bb.setByte(position + 2, value & 0xFF);
  }
}
