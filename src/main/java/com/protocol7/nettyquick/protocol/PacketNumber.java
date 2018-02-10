package com.protocol7.nettyquick.protocol;

import com.google.common.primitives.Longs;
import io.netty.buffer.ByteBuf;

public class PacketNumber {

  public static PacketNumber init() {
    return new PacketNumber(0);
  }

  public static PacketNumber read(final ByteBuf bb) {
    return new PacketNumber(bb.readLong());
  }

  private final long number;

  public PacketNumber(final long value) {
    this.number = value;
  }

  public PacketNumber next() {
    return new PacketNumber(number + 1);
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(Longs.toByteArray(number));
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
}
