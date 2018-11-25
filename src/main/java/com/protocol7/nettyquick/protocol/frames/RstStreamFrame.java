package com.protocol7.nettyquick.protocol.frames;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class RstStreamFrame extends Frame {

  public static RstStreamFrame parse(final ByteBuf bb) {
    bb.readByte();

    final StreamId streamId = StreamId.parse(bb);
    final int applicationErrorCode = bb.readUnsignedShort();
    final long offset = Varint.readAsLong(bb);

    return new RstStreamFrame(streamId, applicationErrorCode, offset);
  }

  private final StreamId streamId;
  private final int applicationErrorCode;
  private final long offset;

  public RstStreamFrame(
      final StreamId streamId, final int applicationErrorCode, final long offset) {
    super(FrameType.RST_STREAM);

    requireNonNull(streamId);
    validateApplicationErrorCode(applicationErrorCode);

    this.streamId = streamId;
    this.applicationErrorCode = applicationErrorCode;
    this.offset = offset;
  }

  private void validateApplicationErrorCode(final int code) {
    Preconditions.checkArgument(code >= 0);
    Preconditions.checkArgument(code <= 0xFFFF);
  }

  public StreamId getStreamId() {
    return streamId;
  }

  public int getApplicationErrorCode() {
    return applicationErrorCode;
  }

  public long getOffset() {
    return offset;
  }

  @Override
  public void write(final ByteBuf bb) {
    bb.writeByte(0x01);

    streamId.write(bb);
    bb.writeShort(applicationErrorCode);
    Varint.write(offset, bb);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final RstStreamFrame that = (RstStreamFrame) o;

    if (applicationErrorCode != that.applicationErrorCode) return false;
    if (offset != that.offset) return false;
    return streamId.equals(that.streamId);
  }

  @Override
  public int hashCode() {
    int result = streamId.hashCode();
    result = 31 * result + applicationErrorCode;
    result = 31 * result + (int) (offset ^ (offset >>> 32));
    return result;
  }
}
