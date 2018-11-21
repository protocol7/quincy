package com.protocol7.nettyquick.protocol.frames;

import com.google.common.base.Preconditions;
import com.protocol7.nettyquick.protocol.PacketNumber;

public class AckBlock {

  public static AckBlock fromLongs(long smallest, long largest) {
    return new AckBlock(new PacketNumber(smallest),
                        new PacketNumber(largest));
  }

  private final PacketNumber smallest;
  private final PacketNumber largest;

  public AckBlock(final PacketNumber smallest, final PacketNumber largest) {
    Preconditions.checkArgument(largest.compareTo(smallest) >= 0);

    this.smallest = smallest;
    this.largest = largest;
  }

  public AckBlock(final int smallest, final int largest) {
    this(new PacketNumber(smallest), new PacketNumber(largest));
  }

  public PacketNumber getSmallest() {
    return smallest;
  }

  public PacketNumber getLargest() {
    return largest;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final AckBlock ackBlock = (AckBlock) o;

    if (smallest != null ? !smallest.equals(ackBlock.smallest) : ackBlock.smallest != null) return false;
    return largest != null ? largest.equals(ackBlock.largest) : ackBlock.largest == null;

  }

  @Override
  public int hashCode() {
    int result = smallest != null ? smallest.hashCode() : 0;
    result = 31 * result + (largest != null ? largest.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AckBlock[" +
            smallest +
            ".." + largest +
            ']';
  }
}
