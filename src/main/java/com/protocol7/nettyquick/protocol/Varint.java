package com.protocol7.nettyquick.protocol;

import com.google.common.primitives.Longs;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public class Varint implements Comparable<Varint> {

  public static final long MAX = 4611686018427387903L;

  public static Varint random() {
    return random(0);
  }

  public static Varint random(int min) {
    return new Varint(Rnd.rndInt(min, Integer.MAX_VALUE));
  }

  public static Varint read(ByteBuf bb) {
    int first = (bb.readByte() & 0xFF);
    int size = ((first & 0b11000000) & 0xFF) ;
    int rest = ((first & 0b00111111) & 0xFF) ;

    int len = (int)Math.pow(2, size >> 6) - 1;

    byte[] b = new byte[len];
    bb.readBytes(b);
    byte[] pad = new byte[7-len];
    byte[] bs = Bytes.concat(pad, new byte[]{(byte)rest}, b);

    return new Varint(Longs.fromByteArray(bs));
  }

  public static void write(long value, ByteBuf bb) {
    new Varint(value).write(bb);
  }

  public Varint(final long value) {
    if (value < 0 || value > MAX) {
      throw new IllegalArgumentException("Varint out of bounds: " + value);
    }
    this.value = value;
  }

  private final long value;

  public long getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final Varint varint = (Varint) o;

    return value == varint.value;

  }

  @Override
  public int hashCode() {
    return (int) (value ^ (value >>> 32));
  }

  @Override
  public int compareTo(final Varint o) {
    return Longs.compare(this.value, o.value);
  }

  public void write(ByteBuf bb) {
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
    b[0] = (byte)(b[0] | mask);
    bb.writeBytes(b);
  }

  @Override
  public String toString() {
    return Long.toString(value);
  }
}
