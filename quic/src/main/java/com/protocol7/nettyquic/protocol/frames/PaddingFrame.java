package com.protocol7.nettyquic.protocol.frames;

import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class PaddingFrame extends Frame {

  public static PaddingFrame parse(final ByteBuf bb) {
    int length = 0;
    while (bb.isReadable()) {
      final int pos = bb.readerIndex();
      final byte b = bb.readByte();
      if (b == 0) {
        length += 1;
      } else {
        bb.readerIndex(pos);
        break;
      }
    }

    if (length == 0) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    return new PaddingFrame(length);
  }

  private final int length;

  public PaddingFrame(final int length) {
    super(FrameType.PADDING);
    if (length < 1) {
      throw new IllegalArgumentException("Length must be at least 1");
    }

    this.length = length;
  }

  @Override
  public int calculateLength() {
    return length;
  }

  @Override
  public void write(final ByteBuf bb) {
    final byte[] b = new byte[length];
    bb.writeBytes(b);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final PaddingFrame that = (PaddingFrame) o;
    return length == that.length;
  }

  @Override
  public int hashCode() {
    return Objects.hash(length);
  }

  @Override
  public String toString() {
    return "PaddingFrame(" + length + ")";
  }
}
