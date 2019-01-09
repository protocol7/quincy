package com.protocol7.nettyquic.protocol.frames;

import com.protocol7.nettyquic.Writeable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public abstract class Frame implements Writeable {

  public static Frame parse(final ByteBuf bb) {
    byte typeByte = bb.getByte(bb.readerIndex());
    FrameType type = FrameType.fromByte(typeByte);

    if (type == FrameType.STREAM) {
      return StreamFrame.parse(bb);
    } else if (type == FrameType.PADDING) {
      return PaddingFrame.parse(bb);
    } else if (type == FrameType.CRYPTO) {
      return CryptoFrame.parse(bb);
    } else if (type == FrameType.ACK) {
      return AckFrame.parse(bb);
    } else if (type == FrameType.PING) {
      return PingFrame.parse(bb);
    } else if (type == FrameType.RETIRE_CONNECTION_ID) {
      return RetireConnectionIdFrame.parse(bb);
    } else if (type == FrameType.RESET_STREAM) {
      return ResetStreamFrame.parse(bb);
    } else if (type == FrameType.CONNECTION_CLOSE) {
      return ConnectionCloseFrame.parse(bb);
    } else if (type == FrameType.MAX_STREAM_DATA) {
      return MaxStreamDataFrame.parse(bb);
    } else {
      throw new RuntimeException("Unknown frame type " + type);
    }
  }

  private final FrameType type;

  public Frame(final FrameType type) {
    this.type = type;
  }

  public FrameType getType() {
    return type;
  }

  public int calculateLength() {
    // TODO implement in subclasses, this is slow
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
