package com.protocol7.quincy.protocol.frames;

import io.netty.buffer.ByteBuf;

public class HandshakeDoneFrame extends Frame {

  public static final HandshakeDoneFrame INSTANCE = new HandshakeDoneFrame();

  public static HandshakeDoneFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.HANDSHAKE_DONE.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    return INSTANCE;
  }

  private HandshakeDoneFrame() {
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
    return "HandshakeDoneFrame";
  }
}
