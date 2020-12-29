package com.protocol7.quincy.protocol.frames;

import com.protocol7.quincy.Varint;
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
    final long maxStreams = Varint.readAsLong(bb);

    return new StreamsBlockedFrame(maxStreams, bidi);
  }

  private final long maxStreams;
  private final boolean bidi;

  public StreamsBlockedFrame(final long maxStreams, final boolean bidi) {
    super(FrameType.STREAMS_BLOCKED);

    this.maxStreams = maxStreams;
    this.bidi = bidi;
  }

  public long getMaxStreams() {
    return maxStreams;
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

    Varint.write(maxStreams, bb);
  }
}
