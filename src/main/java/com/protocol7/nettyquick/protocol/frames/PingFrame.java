package com.protocol7.nettyquick.protocol.frames;

import io.netty.buffer.ByteBuf;

public class PingFrame extends Frame {

  public static final PingFrame INSTANCE = new PingFrame();

  public static PingFrame parse(ByteBuf bb) {
    bb.readByte();

    return INSTANCE;
  }

  private PingFrame() {
    super(FrameType.PING);
  }

  @Override
  public int calculateLength() {
    return 1;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());
  }
}
