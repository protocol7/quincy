package com.protocol7.quincy;

import com.google.common.primitives.Longs;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class Varint {

  public static final long MAX = 4611686018427387903L;

  public static long random() {
    return random(0);
  }

  public static long random(final int min) {
    return Rnd.rndInt(min, Integer.MAX_VALUE);
  }

  public static long readAsLong(final ByteBuf bb) {
    return read(bb);
  }

  public static long readAsLong(final byte[] b) {
    return read(b);
  }

  public static int readAsInt(final ByteBuf bb) {
    return (int) read(bb);
  }

  public static int readAsInt(final byte[] b) {
    return (int) read(b);
  }

  public static byte readAsByte(final ByteBuf bb) {
    return (byte) read(bb);
  }

  private static long read(final ByteBuf bb) {
    final int first = (bb.readByte() & 0xFF);
    final int size = ((first & 0b11000000) & 0xFF);
    final int rest = ((first & 0b00111111) & 0xFF);

    final int len = (int) Math.pow(2, size >> 6) - 1;

    final byte[] b = new byte[len];
    bb.readBytes(b);
    final byte[] pad = new byte[7 - len];
    final byte[] bs = Bytes.concat(pad, new byte[] {(byte) rest}, b);

    final long value = Longs.fromByteArray(bs);

    checkRange(value);

    return value;
  }

  private static long read(final byte[] b) {
    final int first = (b[0] & 0xFF);
    final int size = ((first & 0b11000000) & 0xFF);
    final int rest = ((first & 0b00111111) & 0xFF);

    final int len = (int) Math.pow(2, size >> 6) - 1;

    if (b.length != (len + 1)) {
      throw new IllegalArgumentException("buffer not of correct length");
    }

    final byte[] pad = new byte[7 - len];
    final byte[] bs =
        Bytes.concat(pad, new byte[] {(byte) rest}, Arrays.copyOfRange(b, 1, b.length));

    final long value = Longs.fromByteArray(bs);

    checkRange(value);

    return value;
  }

  public static void write(final long value, final ByteBuf bb) {
    checkRange(value);

    bb.writeBytes(write(value));
  }

  public static byte[] write(final long value) {
    final int from;
    final int mask;
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
    final byte[] bs = Longs.toByteArray(value);
    final byte[] b = Arrays.copyOfRange(bs, from, 8);
    b[0] = (byte) (b[0] | mask);
    return b;
  }

  private static void checkRange(final long value) {
    if (value < 0 || value > MAX) {
      throw new IllegalArgumentException("Varint out of bounds: " + value);
    }
  }
}
