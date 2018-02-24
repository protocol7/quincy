package com.protocol7.nettyquick.protocol.frames;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

public class PingFrame extends Frame {

  public static PingFrame parse(ByteBuf bb) {
    bb.readByte();

    int length = bb.readByte();

    byte[] data = new byte[length];
    bb.readBytes(data);

    return new PingFrame(data);
  }

  private final byte[] data;

  public PingFrame(final byte[] data) {
    super(FrameType.PADDING);

    Preconditions.checkArgument(data.length < 256);

    this.data = data;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public int calculateLength() {
    return 1 + data.length;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(getType().getType());

    bb.writeByte(data.length);
    bb.writeBytes(data);
  }
}
