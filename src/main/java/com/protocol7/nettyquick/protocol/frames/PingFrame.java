package com.protocol7.nettyquick.protocol.frames;

import java.util.Arrays;

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
    super(FrameType.PING);

    Preconditions.checkArgument(data.length < 256);

    this.data = data;
  }

  public PingFrame() {
    this(new byte[0]);
  }

  public byte[] getData() {
    return data;
  }

  public boolean isEmpty() {
    return data.length == 0;
  }

  @Override
  public int calculateLength() {
    return 2 + data.length;
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

    final PingFrame pingFrame = (PingFrame) o;

    return Arrays.equals(data, pingFrame.data);

  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }
}
