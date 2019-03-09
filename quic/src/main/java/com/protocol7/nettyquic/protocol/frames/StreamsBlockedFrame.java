package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.Varint;
import io.netty.buffer.ByteBuf;

public class StreamsBlockedFrame extends Frame {

  private static final byte BIDI_TYPE = 0x16;
  private static final byte UNI_TYPE = 0x17;

  public static StreamsBlockedFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != BIDI_TYPE && type != UNI_TYPE) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final boolean bidi = type == BIDI_TYPE;
    final long streamLimit = Varint.readAsLong(bb);

    return new StreamsBlockedFrame(streamLimit, bidi);
  }

  private final long streamsLimit;
  private final boolean bidi;

  public StreamsBlockedFrame(final long streamsLimit, final boolean bidi) {
    super(FrameType.STREAMS_BLOCKED);

    this.streamsLimit = streamsLimit;
    this.bidi = bidi;
  }

  public long getStreamsLimit() {
    return streamsLimit;
  }

  public boolean isBidi() {
    return bidi;
  }

  @Override
  public void write(final ByteBuf bb) {
    if (bidi) {
      bb.writeByte(BIDI_TYPE);
    } else {
      bb.writeByte(UNI_TYPE);
    }

    Varint.write(streamsLimit, bb);
  }
}
