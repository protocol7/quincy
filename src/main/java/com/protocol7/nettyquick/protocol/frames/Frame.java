package com.protocol7.nettyquick.protocol.frames;

import io.netty.buffer.ByteBuf;

public abstract class Frame {

  public static Frame parse(ByteBuf bb) {
    byte typeByte = bb.getByte(bb.readerIndex());
    FrameType type = FrameType.fromByte(typeByte);

    if (type == FrameType.STREAM) {
      return StreamFrame.parse(bb);
    } else if (type == FrameType.PADDING) {
      return PaddingFrame.parse(bb);
    }

    return null;
  }

  private final FrameType type;

  public Frame(final FrameType type) {
    this.type = type;
  }

  public FrameType getType() {
    return type;
  }

  public abstract int getLength();

  public abstract void write(ByteBuf bb);
}
