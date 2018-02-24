package com.protocol7.nettyquick.protocol.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public abstract class Frame {

  public static Frame parse(ByteBuf bb) {
    byte typeByte = bb.getByte(bb.readerIndex());
    FrameType type = FrameType.fromByte(typeByte);

    if (type == FrameType.STREAM) {
      return StreamFrame.parse(bb);
    } else if (type == FrameType.PADDING) {
      return PaddingFrame.parse(bb);
    } else if (type == FrameType.ACK) {
      return AckFrame.parse(bb);
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

  public int calculateLength() {
    // TODO improve
    ByteBuf bb = Unpooled.buffer();
    try {
      write(bb);
      return bb.writerIndex();
    } finally {
      bb.release();
    }
  }

  public abstract void write(ByteBuf bb);
}
