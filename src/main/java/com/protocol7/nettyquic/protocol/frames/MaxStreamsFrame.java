package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class MaxStreamsFrame extends Frame {

  public static MaxStreamsFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.MAX_STREAMS.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final long maxStreams = Varint.readAsLong(bb);

    return new MaxStreamsFrame(maxStreams);
  }

  private final long maxStreams;

  public MaxStreamsFrame(final long maxStreams) {
    super(FrameType.MAX_STREAMS);

    this.maxStreams = maxStreams;
  }

  public long getMaxStreams() {
    return maxStreams;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    Varint.write(maxStreams, bb);
  }
}
