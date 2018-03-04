package com.protocol7.nettyquick.protocol.frames;

import java.util.Arrays;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;

public class PongFrame extends Frame {

  public static PongFrame parse(ByteBuf bb) {
    bb.readByte();

    int length = bb.readByte();

    byte[] data = new byte[length];
    bb.readBytes(data);

    return new PongFrame(data);
  }

  private final byte[] data;

  public PongFrame(final byte[] data) {
    super(FrameType.PONG);

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

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PongFrame pingFrame = (PongFrame) o;

    return Arrays.equals(data, pingFrame.data);

  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }
}
