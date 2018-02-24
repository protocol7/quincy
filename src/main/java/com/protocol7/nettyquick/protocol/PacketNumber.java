package com.protocol7.nettyquick.protocol;

import com.google.common.primitives.Longs;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;

public class PacketNumber implements Comparable<PacketNumber> {

  public static PacketNumber random() {
    return new PacketNumber(Rnd.rndLong(0, Varint.MAX));
  }

  public static PacketNumber read(final ByteBuf bb) {
    return new PacketNumber(bb.readLong());
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
    asVarint().write(bb);
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
