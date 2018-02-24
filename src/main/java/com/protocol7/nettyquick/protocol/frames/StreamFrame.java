package com.protocol7.nettyquick.protocol.frames;

import java.util.Arrays;

import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Varint;
import io.netty.buffer.ByteBuf;

public class StreamFrame extends Frame {

  public static StreamFrame parse(ByteBuf bb) {
    byte firstByte = bb.readByte();

    boolean off = (firstByte & 0x04) == 0x04;
    boolean len = (firstByte & 0x02) == 0x02;
    boolean fin = (firstByte & 0x01) == 0x01;

    StreamId streamId = StreamId.parse(bb);
    Varint offset;
    if (off) {
      offset = Varint.read(bb);
    } else {
      offset = new Varint(0);
    }

    int length;
    if (len) {
      length = (int)Varint.read(bb).getValue();
    } else {
      length = bb.readableBytes();
    }

    byte[] data = new byte[length];
    bb.readBytes(data);

    return new StreamFrame(streamId, offset.getValue(), fin, data);
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
      new Varint(offset).write(bb);
    }

    new Varint(data.length).write(bb);

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
