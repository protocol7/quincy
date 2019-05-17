package com.protocol7.quincy.protocol.frames;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class MaxStreamDataFrame extends Frame {

  public static MaxStreamDataFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.MAX_STREAM_DATA.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long streamId = StreamId.parse(bb);
    final long maxStreamData = Varint.readAsLong(bb);

    return new MaxStreamDataFrame(streamId, maxStreamData);
  }

  private final long streamId;
  private final long maxStreamData;

  public MaxStreamDataFrame(final long streamId, final long maxStreamData) {
    super(FrameType.MAX_STREAM_DATA);

    requireNonNull(streamId);

    this.streamId = StreamId.validate(streamId);
    this.maxStreamData = maxStreamData;
  }

  public long getStreamId() {
    return streamId;
  }

  public long getMaxStreamData() {
    return maxStreamData;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    StreamId.write(bb, streamId);
    Varint.write(maxStreamData, bb);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final MaxStreamDataFrame that = (MaxStreamDataFrame) o;
    return maxStreamData == that.maxStreamData && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(streamId, maxStreamData);
  }

  @Override
  public String toString() {
    return "MaxStreamDataFrame{"
        + "streamId="
        + streamId
        + ", maxStreamData="
        + maxStreamData
        + '}';
  }
}
