package com.protocol7.quincy.protocol.frames;

import com.protocol7.quincy.Varint;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

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

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final MaxDataFrame that = (MaxDataFrame) o;
    return maxData == that.maxData;
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxData);
  }

  @Override
  public String toString() {
    return "MaxDataFrame{" + "maxData=" + maxData + '}';
  }
}
