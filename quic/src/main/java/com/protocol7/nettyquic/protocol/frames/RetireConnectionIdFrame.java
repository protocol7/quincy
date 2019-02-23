package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class RetireConnectionIdFrame extends Frame {

  private final long sequenceNumber;

  public static RetireConnectionIdFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.RETIRE_CONNECTION_ID.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long sequenceNumber = Varint.readAsLong(bb);

    return new RetireConnectionIdFrame(sequenceNumber);
  }

  public RetireConnectionIdFrame(final long sequenceNumber) {
    super(FrameType.RETIRE_CONNECTION_ID);
    this.sequenceNumber = sequenceNumber;
  }

  public long getSequenceNumber() {
    return sequenceNumber;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    Varint.write(sequenceNumber, bb);
  }
}
