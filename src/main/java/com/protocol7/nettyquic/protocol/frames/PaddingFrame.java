package com.protocol7.nettyquic.protocol.frames;

import io.netty.buffer.ByteBuf;

public class PaddingFrame extends Frame {

  public static PaddingFrame parse(final ByteBuf bb) {
    byte type = bb.readByte();
    if (type != FrameType.PADDING.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    return INSTANCE;
  }

  public static final PaddingFrame INSTANCE = new PaddingFrame();

  private PaddingFrame() {
    super(FrameType.PADDING);
  }

  @Override
  public int calculateLength() {
    return 1;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());
  }

  @Override
  public String toString() {
    return "PaddingFrame";
  }
}
