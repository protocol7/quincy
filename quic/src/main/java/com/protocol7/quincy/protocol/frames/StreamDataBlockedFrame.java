package com.protocol7.quincy.protocol.frames;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class StreamDataBlockedFrame extends Frame {

  public static StreamDataBlockedFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.STREAM_DATA_BLOCKED.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long streamId = StreamId.parse(bb);
    final long streamDataLimit = Varint.readAsLong(bb);

    return new StreamDataBlockedFrame(streamId, streamDataLimit);
  }

  private final long streamId;
  private final long streamDataLimit;

  public StreamDataBlockedFrame(final long streamId, final long streamDataLimit) {
    super(FrameType.STREAM_DATA_BLOCKED);

    requireNonNull(streamId);

    this.streamId = StreamId.validate(streamId);
    this.streamDataLimit = streamDataLimit;
  }

  public long getStreamId() {
    return streamId;
  }

  public long getStreamDataLimit() {
    return streamDataLimit;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    StreamId.write(bb, streamId);
    Varint.write(streamDataLimit, bb);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final StreamDataBlockedFrame that = (StreamDataBlockedFrame) o;
    return streamDataLimit == that.streamDataLimit && Objects.equals(streamId, that.streamId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(streamId, streamDataLimit);
  }
}
