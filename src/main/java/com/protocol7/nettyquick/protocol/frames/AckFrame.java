package com.protocol7.nettyquick.protocol.frames;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class AckFrame extends Frame {

  public static AckFrame parse(ByteBuf bb) {
    bb.readByte();

    final long largestAcknowledged = Varint.read(bb).getValue();
    final Varint ackDelay = Varint.read(bb);
    final long blockCount = Varint.read(bb).getValue();

    final List<AckBlock> blocks = Lists.newArrayList();

    final Varint firstBlock = Varint.read(bb);
    long smallest = largestAcknowledged - firstBlock.getValue();

    blocks.add(AckBlock.fromLongs(smallest, largestAcknowledged));


    long largest = largestAcknowledged;
    for (int i = 0; i<blockCount; i++) {
      System.out.println(largest + " - " + smallest);
      if (i % 2 == 0) {
        // reading gap
        Varint gap = Varint.read(bb);
        largest = smallest - gap.getValue() - 1;
      } else {
        Varint ackBlock = Varint.read(bb);
        smallest = largest - ackBlock.getValue();
        blocks.add(AckBlock.fromLongs(smallest, largest));
      }
    }

    return new AckFrame(ackDelay.getValue(), blocks);
  }

  private final long ackDelay;
  private final List<AckBlock> blocks;

  public AckFrame(final long ackDelay, final AckBlock... blocks) {
    this(ackDelay, Arrays.asList(blocks));
  }

  public AckFrame(final long ackDelay, final List<AckBlock> blocks) {
    super(FrameType.ACK);

    Objects.requireNonNull(ackDelay);
    Objects.requireNonNull(blocks);

    this.ackDelay = ackDelay;
    this.blocks = orderBlocks(blocks);
  }

  private List<AckBlock> orderBlocks(List<AckBlock> blocks) {
    if (blocks.size() < 1) {
      throw new IllegalArgumentException("Must contain at least one block");
    }

    List<AckBlock> sorted = Lists.newArrayList(blocks);
    sorted.sort((b1, b2) -> b2.getLargest().compareTo(b1.getLargest()));

    // TODO check overlaps

    return ImmutableList.copyOf(sorted);
  }

  public long getAckDelay() {
    return ackDelay;
  }

  public List<AckBlock> getBlocks() {
    return blocks;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    AckBlock firstBlock = blocks.get(0);

    firstBlock.getLargest().writeVarint(bb);
    new Varint(ackDelay).write(bb);
    new Varint((blocks.size() - 1) * 2).write(bb);

    long largest = firstBlock.getLargest().asLong();
    long smallest = firstBlock.getSmallest().asLong();
    new Varint(largest - smallest).write(bb);

    for (int i = 1; i<blocks.size(); i++) {
      AckBlock block = blocks.get(i);

      long gap = smallest - block.getLargest().asLong() - 1;
      new Varint(gap).write(bb);

      long nextBlock = block.getLargest().asLong() - block.getSmallest().asLong();
      smallest = block.getSmallest().asLong();
      new Varint(nextBlock).write(bb);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final AckFrame ackFrame = (AckFrame) o;

    if (ackDelay != ackFrame.ackDelay) return false;
    return blocks.equals(ackFrame.blocks);

  }

  @Override
  public int hashCode() {
    int result = (int) (ackDelay ^ (ackDelay >>> 32));
    result = 31 * result + blocks.hashCode();
    return result;
  }
}
