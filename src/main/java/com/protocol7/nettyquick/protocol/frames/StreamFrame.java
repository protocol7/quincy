package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public class StreamFrame extends Frame {

  public static StreamFrame parse(final ByteBuf bb) {
    final byte firstByte = bb.readByte();

    final boolean off = (firstByte & 0x04) == 0x04;
    final boolean len = (firstByte & 0x02) == 0x02;
    final boolean fin = (firstByte & 0x01) == 0x01;

    final StreamId streamId = StreamId.parse(bb);
    final Varint offset;
    if (off) {
      offset = Varint.read(bb);
    } else {
      offset = new Varint(0);
    }

    final int length;
    if (len) {
      length = (int)Varint.read(bb).longValue();
    } else {
      length = bb.readableBytes();
    }

    final byte[] data = new byte[length];
    bb.readBytes(data);

    return new StreamFrame(streamId, offset.longValue(), fin, data);
  }

  private final StreamId streamId;
  private final long offset;
  private final boolean fin;
  private final byte[] data;

  public StreamFrame(final StreamId streamId, final long offset, final boolean fin, final byte[] data) {
    super(FrameType.STREAM);
    this.streamId = streamId;
    this.offset = offset;
    this.fin = fin;
    this.data = data;
  }

  public StreamId getStreamId() {
    return streamId;
  }

  public long getOffset() {
    return offset;
  }

  public boolean isFin() {
    return fin;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public void write(final ByteBuf bb) {
    byte type = getType().getType();
    if (offset > 0) {
      type = (byte)(type | 0x04);
    }
    if (isFin()) {
      type = (byte)(type | 0x01);
    }
    // TODO only set len when needed
    type = (byte)(type | 0x02);

    bb.writeByte(type);
    streamId.write(bb);
    if (offset > 0) {
      Varint.write(offset, bb);
    }

    Varint.write(data.length, bb);

    bb.writeBytes(data);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final StreamFrame that = (StreamFrame) o;

    if (offset != that.offset) return false;
    if (fin != that.fin) return false;
    if (!streamId.equals(that.streamId)) return false;
    return Arrays.equals(data, that.data);

  }

  @Override
  public int hashCode() {
    int result = streamId.hashCode();
    result = 31 * result + (int) (offset ^ (offset >>> 32));
    result = 31 * result + (fin ? 1 : 0);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }

  @Override
  public String toString() {
    return "StreamFrame{" +
            "streamId=" + streamId +
            ", offset=" + offset +
            ", fin=" + fin +
            ", data=" + Arrays.toString(data) +
            '}';
  }
}
