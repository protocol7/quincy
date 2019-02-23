package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class DataBlockedFrame extends Frame {

  public static DataBlockedFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.DATA_BLOCKED.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long dataLimit = Varint.readAsLong(bb);

    return new DataBlockedFrame(dataLimit);
  }

  private final long dataLimit;

  public DataBlockedFrame(final long dataLimit) {
    super(FrameType.DATA_BLOCKED);

    this.dataLimit = dataLimit;
  }

  public long getDataLimit() {
    return dataLimit;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    Varint.write(dataLimit, bb);
  }
}
