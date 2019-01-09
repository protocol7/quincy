package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class MaxDataFrame extends Frame {

  public static MaxDataFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.MAX_DATA.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long maxData = Varint.readAsLong(bb);

    return new MaxDataFrame(maxData);
  }

  private final long maxData;

  public MaxDataFrame(final long maxData) {
    super(FrameType.MAX_DATA);

    this.maxData = maxData;
  }

  public long getMaxData() {
    return maxData;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    Varint.write(maxData, bb);
  }
}
