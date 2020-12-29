package com.protocol7.quincy.protocol.frames;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.Varint;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AckFrame extends Frame {

  public static AckFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.ACK.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long largestAcknowledged = Varint.readAsLong(bb);
    final long ackDelay = Varint.readAsLong(bb);
    final long blockCount = Varint.readAsLong(bb);

    final List<AckRange> ranges = new ArrayList<>();

    final long firstRange = Varint.readAsLong(bb);
    long smallest = largestAcknowledged - firstRange;

    ranges.add(new AckRange(smallest, largestAcknowledged));

    long largest = largestAcknowledged;
    for (int i = 0; i < blockCount; i++) {
      if (i % 2 == 0) {
        // reading gap
        final long gap = Varint.readAsLong(bb);
        largest = smallest - gap - 1;
      } else {
        final long ackRange = Varint.readAsLong(bb);
        smallest = largest - ackRange;
        ranges.add(new AckRange(smallest, largest));
      }
    }

    return new AckFrame(ackDelay, ranges);
  }

  private final long ackDelay;
  private final List<AckRange> ranges;

  public AckFrame(final long ackDelay, final AckRange... ranges) {
    this(ackDelay, Arrays.asList(ranges));
  }

  public AckFrame(final long ackDelay, final List<AckRange> ranges) {
    super(FrameType.ACK);

    checkArgument(ackDelay >= 0);
    requireNonNull(ranges);
    checkArgument(ranges.size() > 0);

    this.ackDelay = ackDelay;
    this.ranges = orderRanges(ranges);
  }

  private List<AckRange> orderRanges(final List<AckRange> ranges) {
    if (ranges.size() < 1) {
      throw new IllegalArgumentException("Must contain at least one block");
    }

    final List<AckRange> sorted = new ArrayList<>(ranges);
    sorted.sort((b1, b2) -> Long.compare(b2.getLargest(), b1.getLargest()));

    // TODO check overlaps

    return List.copyOf(sorted);
  }

  public long getAckDelay() {
    return ackDelay;
  }

  public List<AckRange> getRanges() {
    return ranges;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    final AckRange firstRange = ranges.get(0);

    Varint.write(firstRange.getLargest(), bb);
    Varint.write(ackDelay, bb);
    Varint.write((ranges.size() - 1) * 2, bb);

    final long largest = firstRange.getLargest();
    long smallest = firstRange.getSmallest();
    Varint.write(largest - smallest, bb);

    for (int i = 1; i < ranges.size(); i++) {
      final AckRange range = ranges.get(i);

      final long gap = smallest - range.getLargest() - 1;
      Varint.write(gap, bb);

      final long nextRange = range.getLargest() - range.getSmallest();
      smallest = range.getSmallest();
      Varint.write(nextRange, bb);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final AckFrame ackFrame = (AckFrame) o;

    if (ackDelay != ackFrame.ackDelay) return false;
    return ranges.equals(ackFrame.ranges);
  }

  @Override
  public int hashCode() {
    int result = (int) (ackDelay ^ (ackDelay >>> 32));
    result = 31 * result + ranges.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "AckFrame{" + "ackDelay=" + ackDelay + ", ranges=" + ranges + '}';
  }
}
