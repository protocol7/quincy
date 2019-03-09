package com.protocol7.nettyquic.protocol.frames;

import static java.util.Objects.requireNonNull;

import com.protocol7.nettyquic.Varint;
import com.protocol7.nettyquic.protocol.StreamId;
import io.netty.buffer.ByteBuf;

public class StreamDataBlockedFrame extends Frame {

  public static StreamDataBlockedFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.STREAM_DATA_BLOCKED.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final StreamId streamId = StreamId.parse(bb);
    final long streamDataLimit = Varint.readAsLong(bb);

    return new StreamDataBlockedFrame(streamId, streamDataLimit);
  }

  private final StreamId streamId;
  private final long streamDataLimit;

  public StreamDataBlockedFrame(final StreamId streamId, final long streamDataLimit) {
    super(FrameType.STREAM_DATA_BLOCKED);

    requireNonNull(streamId);

    this.streamId = streamId;
    this.streamDataLimit = streamDataLimit;
  }

  public StreamId getStreamId() {
    return streamId;
  }

  public long getStreamDataLimit() {
    return streamDataLimit;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    streamId.write(bb);
    Varint.write(streamDataLimit, bb);
  }
}
