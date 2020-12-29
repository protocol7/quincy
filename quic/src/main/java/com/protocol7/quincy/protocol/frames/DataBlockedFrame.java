package com.protocol7.quincy.protocol.frames;

import com.protocol7.quincy.Varint;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class DataBlockedFrame extends Frame {

  public static DataBlockedFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.DATA_BLOCKED.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long maxData = Varint.readAsLong(bb);

    return new DataBlockedFrame(maxData);
  }

  private final long maxData;

  public DataBlockedFrame(final long maxData) {
    super(FrameType.DATA_BLOCKED);

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

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final DataBlockedFrame that = (DataBlockedFrame) o;
    return maxData == that.maxData;
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxData);
  }
}
