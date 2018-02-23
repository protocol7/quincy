package com.protocol7.nettyquick.protocol.frames;

import io.netty.buffer.ByteBuf;

public class PaddingFrame extends Frame {

  public static PaddingFrame parse(ByteBuf bb) {
    bb.readByte();

    return INSTANCE;
  }

  public static final PaddingFrame INSTANCE = new PaddingFrame();

  private PaddingFrame() {
    super(FrameType.PADDING);
  }

  @Override
  public int getLength() {
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
