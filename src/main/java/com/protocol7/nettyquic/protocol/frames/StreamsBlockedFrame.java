package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class StreamsBlockedFrame extends Frame {

  public static StreamsBlockedFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.STREAMS_BLOCKED.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long streamLimit = Varint.readAsLong(bb);

    return new StreamsBlockedFrame(streamLimit);
  }

  private final long streamsLimit;

  public StreamsBlockedFrame(final long streamsLimit) {
    super(FrameType.STREAMS_BLOCKED);

    this.streamsLimit = streamsLimit;
  }

  public long getStreamsLimit() {
    return streamsLimit;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    Varint.write(streamsLimit, bb);
  }
}
