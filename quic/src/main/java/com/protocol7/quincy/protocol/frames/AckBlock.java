package com.protocol7.quincy.protocol.frames;

import static com.google.common.base.Preconditions.checkArgument;

import com.protocol7.quincy.protocol.PacketNumber;
import java.util.Objects;

public class AckBlock {

  private final long smallest;
  private final long largest;

  public AckBlock(final long smallest, final long largest) {
    checkArgument(largest >= smallest);

    this.smallest = PacketNumber.validate(smallest);
    this.largest = PacketNumber.validate(largest);
  }

  public long getSmallest() {
    return smallest;
  }

  public long getLargest() {
    return largest;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final AckBlock ackBlock = (AckBlock) o;
    return smallest == ackBlock.smallest && largest == ackBlock.largest;
  }

  @Override
  public int hashCode() {
    return Objects.hash(smallest, largest);
  }

  @Override
  public String toString() {
    return "AckBlock[" + smallest + ".." + largest + ']';
  }
}
