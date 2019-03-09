package com.protocol7.nettyquic.protocol.frames;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.protocol7.nettyquic.Varint;
import com.protocol7.nettyquic.protocol.StreamId;
import io.netty.buffer.ByteBuf;

public class ResetStreamFrame extends Frame {

  public static ResetStreamFrame parse(final ByteBuf bb) {
    final byte type = bb.readByte();
    if (type != FrameType.RESET_STREAM.getType()) {
      throw new IllegalArgumentException("Illegal frame type");
    }

    final StreamId streamId = StreamId.parse(bb);
    final int applicationErrorCode = bb.readUnsignedShort();
    final long offset = Varint.readAsLong(bb);

    return new ResetStreamFrame(streamId, applicationErrorCode, offset);
  }

  private final StreamId streamId;
  private final int applicationErrorCode;
  private final long offset;

  public ResetStreamFrame(
      final StreamId streamId, final int applicationErrorCode, final long offset) {
    super(FrameType.RESET_STREAM);

    requireNonNull(streamId);
    validateApplicationErrorCode(applicationErrorCode);

    this.streamId = streamId;
    this.applicationErrorCode = applicationErrorCode;
    this.offset = offset;
  }

  private void validateApplicationErrorCode(final int code) {
    checkArgument(code >= 0);
    checkArgument(code <= 0xFFFF);
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
    bb.writeByte(getType().getType());

    streamId.write(bb);
    bb.writeShort(applicationErrorCode);
    Varint.write(offset, bb);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ResetStreamFrame that = (ResetStreamFrame) o;

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
