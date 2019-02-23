package com.protocol7.nettyquic.protocol.frames;

import static java.util.Objects.requireNonNull;

import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class MaxStreamDataFrame extends Frame {

  public static MaxStreamDataFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.MAX_STREAM_DATA.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final StreamId streamId = StreamId.parse(bb);
    final long maxStreamData = Varint.readAsLong(bb);

    return new MaxStreamDataFrame(streamId, maxStreamData);
  }

  private final StreamId streamId;
  private final long maxStreamData;

  public MaxStreamDataFrame(final StreamId streamId, final long maxStreamData) {
    super(FrameType.MAX_STREAM_DATA);

    requireNonNull(streamId);

    this.streamId = streamId;
    this.maxStreamData = maxStreamData;
  }

  public StreamId getStreamId() {
    return streamId;
  }

  public long getMaxStreamData() {
    return maxStreamData;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    streamId.write(bb);
    Varint.write(maxStreamData, bb);
  }
}
