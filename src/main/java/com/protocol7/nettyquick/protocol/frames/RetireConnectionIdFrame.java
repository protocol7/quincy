package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class RetireConnectionIdFrame extends Frame {

  private final long sequenceNumber;

  public static RetireConnectionIdFrame parse(ByteBuf bb) {
    bb.readByte();

    Varint sequenceNumber = Varint.read(bb);

    return new RetireConnectionIdFrame(sequenceNumber.longValue()) ;
  }

  public RetireConnectionIdFrame(long sequenceNumber) {
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
