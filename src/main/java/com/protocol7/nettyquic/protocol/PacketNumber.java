package com.protocol7.nettyquic.protocol;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.protocol7.nettyquic.utils.Bytes;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class PacketNumber implements Comparable<PacketNumber> {

  public static PacketNumber parse(final ByteBuf bb, int length) {
    byte[] b = new byte[length];
    bb.readBytes(b);
    byte[] pad = new byte[4 - length];
    byte[] bs = Bytes.concat(pad, b);

    return new PacketNumber(Ints.fromByteArray(bs));
  }

  public static final PacketNumber MIN = new PacketNumber(0);

  private final long number;

  public PacketNumber(final long number) {
    Preconditions.checkArgument(number >= 0);
    Preconditions.checkArgument(number <= Varint.MAX);

    this.number = number;
  }

  public PacketNumber next() {
    return new PacketNumber(number + 1);
  }

  public PacketNumber max(PacketNumber other) {
    if (this.compareTo(other) > 0) {
      return this;
    } else {
      return other;
    }
  }

  public long asLong() {
    return number;
  }

  public int getLength() {
    return 4; // TODO
  }

  public byte[] write(int length) {
    byte[] b = new byte[length];
    for (int j = length; j > 0; j--) {
      b[length - j] = (byte) ((number >> (8 * (j - 1))) & 0xFF);
    }
    return b;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PacketNumber that = (PacketNumber) o;
    return number == that.number;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number);
  }

  @Override
  public int compareTo(final PacketNumber o) {
    return Long.compare(this.number, o.number);
  }

  @Override
  public String toString() {
    return Long.toString(number);
  }
}
