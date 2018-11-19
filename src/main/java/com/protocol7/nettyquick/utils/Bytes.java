package com.protocol7.nettyquick.utils;

import com.protocol7.nettyquick.Writeable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Bytes {

  public static byte[] drainToArray(ByteBuf bb) {
      try {
          byte[] b = new byte[bb.readableBytes()];
          bb.readBytes(b);
          return b;
      } finally {
          bb.release();
      }
  }

  public static byte[] peekToArray(ByteBuf bb) {
      byte[] b = new byte[bb.readableBytes()];
      bb.markReaderIndex();
      bb.readBytes(b);
      bb.resetReaderIndex();
      return b;
  }


  public static String binary(int i) {
    return Integer.toBinaryString(i);
  }

  public static byte[] concat(byte[]... bs) {
      if (bs.length == 0) {
          return new byte[0];
      } else if (bs.length == 1) {
          return bs[0];
      }
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

  public static byte[] write(Writeable... writeables) {
      ByteBuf bb = Unpooled.buffer();
      for (Writeable writeable : writeables) {
          writeable.write(bb);
      }
      return drainToArray(bb);
  }
}
