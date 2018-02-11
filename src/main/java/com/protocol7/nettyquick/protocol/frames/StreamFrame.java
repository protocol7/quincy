package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.Varint;
import com.protocol7.nettyquick.utils.Hex;
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
    // TODO handle length and fin flag somehow

    byte type = getType().getType();
    if (offset > 0) {
      type = (byte)(type | 0x04);
    }
    // TODO len
    // TODO fin

    System.out.println(type);
    System.out.println(Hex.hex(type));
    bb.writeByte(type);
    streamId.write(bb);
    if (offset > 0) {
      new Varint(offset).write(bb);
    }

    // TODO length

    bb.writeBytes(data);
  }
}
