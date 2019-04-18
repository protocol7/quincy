package com.protocol7.quincy.protocol.frames;

import io.netty.buffer.ByteBuf;

public class PingFrame extends Frame {

  public static final PingFrame INSTANCE = new PingFrame();

  public static PingFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.PING.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

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

  @Override
  public String toString() {
    return "PingFrame";
  }
}
