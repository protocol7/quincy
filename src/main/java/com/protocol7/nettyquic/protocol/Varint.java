package com.protocol7.nettyquic.protocol;

import com.google.common.primitives.Longs;
import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class Varint {

  public static final long MAX = 4611686018427387903L;

  public static long random() {
    return random(0);
  }

  public static long random(int min) {
    return Rnd.rndInt(min, Integer.MAX_VALUE);
  }

  public static long readAsLong(ByteBuf bb) {
    return read(bb);
  }

  public static long readAsLong(byte[] b) {
    return read(b);
  }

  public static int readAsInt(ByteBuf bb) {
    return (int) read(bb);
  }

  public static int readAsInt(byte[] b) {
    return (int) read(b);
  }

  private static long read(ByteBuf bb) {
    int first = (bb.readByte() & 0xFF);
    int size = ((first & 0b11000000) & 0xFF);
    int rest = ((first & 0b00111111) & 0xFF);

    int len = (int) Math.pow(2, size >> 6) - 1;

    byte[] b = new byte[len];
    bb.readBytes(b);
    byte[] pad = new byte[7 - len];
    byte[] bs = Bytes.concat(pad, new byte[] {(byte) rest}, b);

    long value = Longs.fromByteArray(bs);

    checkRange(value);

    return value;
  }

  private static long read(byte[] b) {
    int first = (b[0] & 0xFF);
    int size = ((first & 0b11000000) & 0xFF);
    int rest = ((first & 0b00111111) & 0xFF);

    int len = (int) Math.pow(2, size >> 6) - 1;

    if (b.length != (len + 1)) {
      throw new IllegalArgumentException("buffer not of correct length");
    }

    byte[] pad = new byte[7 - len];
    byte[] bs = Bytes.concat(pad, new byte[] {(byte) rest}, Arrays.copyOfRange(b, 1, b.length));

    long value = Longs.fromByteArray(bs);

    checkRange(value);

    return value;
  }

  public static void write(long value, ByteBuf bb) {
    checkRange(value);

    bb.writeBytes(write(value));
  }

  public static byte[] write(long value) {
    int from;
    int mask;
    if (value > 1073741823) {
      from = 0;
      mask = 0b11000000;
    } else if (value > 16383) {
      from = 4;
      mask = 0b10000000;
    } else if (value > 63) {
      from = 6;
      mask = 0b01000000;
    } else {
      from = 7;
      mask = 0b00000000;
    }
    byte[] bs = Longs.toByteArray(value);
    byte[] b = Arrays.copyOfRange(bs, from, 8);
    b[0] = (byte) (b[0] | mask);
    return b;
  }

  private static void checkRange(long value) {
    if (value < 0 || value > MAX) {
      throw new IllegalArgumentException("Varint out of bounds: " + value);
    }
  }
}
