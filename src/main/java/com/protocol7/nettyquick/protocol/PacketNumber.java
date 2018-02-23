package com.protocol7.nettyquick.protocol;

import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLong;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;

public class PacketNumber implements Comparable<PacketNumber> {

  public static PacketNumber random() {
    return new PacketNumber(Rnd.rndLong()); // TODO ensure random boundary
  }

  public static PacketNumber read(final ByteBuf bb) {
    return new PacketNumber(bb.readLong());
  }

  private final long number;

  public PacketNumber(final long number) {
    this.number = number;
  }

  public PacketNumber next() {
    return new PacketNumber(number + 1);
  }

  public long asLong() {
    return number;
  }

  public Varint asVarint() {
    return new Varint(number);
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(Longs.toByteArray(number));
  }

  public void writeVarint(final ByteBuf bb) {
    asVarint().write(bb);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PacketNumber that = (PacketNumber) o;

    return number == that.number;

  }

  @Override
  public int hashCode() {
    return (int) (number ^ (number >>> 32));
  }

  @Override
  public int compareTo(final PacketNumber o) {
    return Longs.compare(this.number, o.number);
  }

  @Override
  public String toString() {
    return "PacketNumber(" +
            number +
            ')';
  }
}
