package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class RetireConnectionIdFrame extends Frame {

  private final Varint sequenceNumber;

  public static RetireConnectionIdFrame parse(ByteBuf bb) {
    bb.readByte();

    Varint sequenceNumber = Varint.read(bb);

    return new RetireConnectionIdFrame(sequenceNumber);
  }

  public RetireConnectionIdFrame(Varint sequenceNumber) {
    super(FrameType.RETIRE_CONNECTION_ID);
    this.sequenceNumber = sequenceNumber;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    sequenceNumber.write(bb);

  }
}
