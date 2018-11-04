package com.protocol7.nettyquick.protocol;

import java.util.Arrays;

import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;

public class PacketNumber implements Comparable<PacketNumber> {

  private static final int INITIAL_MAX = 1073741823; // TODO fix

  public static PacketNumber random() {
    return new PacketNumber(Rnd.rndLong(0, INITIAL_MAX));
  }

  public static PacketNumber parseVarint(final ByteBuf bb) {
    int first = (bb.readByte() & 0xFF);
    int size = ((first & 0b11000000) & 0xFF) ;
    int rest = ((first & 0b00111111) & 0xFF) ;

    int len;
    if (size == 0b10000000) {
      len = 1;
    } else if (size == 0b11000000) {
      len = 3;
    } else if (size == 0b0000000) {
      len = 0;
    } else {
      throw new RuntimeException("Unknown size marker");
    }

    //int len = (int)Math.pow(2, size >> 6) - 1;

    byte[] b = new byte[len];
    bb.readBytes(b);
    byte[] pad = new byte[3-len];
    byte[] bs = Bytes.concat(pad, new byte[]{(byte)rest}, b);

    return new PacketNumber(Ints.fromByteArray(bs));
  }


  public static PacketNumber read(final ByteBuf bb) {
    return new PacketNumber(bb.readLong());
  }

  public static PacketNumber read4(final ByteBuf bb, final PacketNumber lastAcked) {
    return readn(bb, 4, lastAcked);
  }

  public static PacketNumber read2(final ByteBuf bb, final PacketNumber lastAcked) {
    return readn(bb, 2, lastAcked);
  }

  public static PacketNumber read1(final ByteBuf bb, final PacketNumber lastAcked) {
    return readn(bb, 1, lastAcked);
  }

  private static PacketNumber readn(final ByteBuf bb, int len, final PacketNumber lastAcked) {
    byte[] b = new byte[len];
    bb.readBytes(b);
    byte[] merged = Longs.toByteArray(lastAcked.asLong());

    // merge the last acked value, with the read suffix
    System.arraycopy(b, 0, merged, 8-len, len);

    long mergedLong = Longs.fromByteArray(merged);

    // the resulting value must be larger than lastAcked. If not, we bump the bytes before until it is, handling byte overflows
    int index = 8 - len - 1;
    while (mergedLong < lastAcked.asLong()) {
      // bump byte
      byte bump = merged[index];
      bump++;
      if (bump != 0) {
        merged[index] = bump;
      } else {
        index--;
      }
      mergedLong = Longs.fromByteArray(merged);
    }

    return new PacketNumber(mergedLong);
  }

  public static final PacketNumber MIN = new PacketNumber(0);

  private final Varint number;

  public PacketNumber(final long number) {
    this.number = new Varint(number);
  }

  public PacketNumber(final Varint number) {
    this.number = number;
  }

  public PacketNumber next() {
    return new PacketNumber(number.getValue() + 1);
  }

  public PacketNumber max(PacketNumber other) {
    if (this.compareTo(other) > 0) {
      return this;
    } else {
      return other;
    }
  }

  public long asLong() {
    return number.getValue();
  }

  public Varint asVarint() {
    return number;
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(Longs.toByteArray(number.getValue()));
  }

  public void writeVarint(final ByteBuf bb) {
    int value = (int)number.getValue();
    int from;
    int mask;
    if (value > 16383) {
      from = 0;
      mask = 0b11000000;
    } else if (value > 63) {
      from = 2;
      mask = 0b10000000;
    } else {
      from = 3;
      mask = 0b00000000;
    }
    byte[] bs = Ints.toByteArray(value);
    byte[] b = Arrays.copyOfRange(bs, from, 4);

    b[0] = (byte)(b[0] | mask);
    bb.writeBytes(b);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PacketNumber that = (PacketNumber) o;

    return number.equals(that.number);
  }

  @Override
  public int hashCode() {
    return number.hashCode();
  }

  @Override
  public int compareTo(final PacketNumber o) {
    return this.number.compareTo(o.number);
  }

  @Override
  public String toString() {
    return number.toString();
  }
}
