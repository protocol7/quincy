package com.protocol7.nettyquick.protocol;

import io.netty.buffer.ByteBuf;

public class StreamId {

  public static StreamId create() {
    return new StreamId(Varint.random());
  }

  public static StreamId parse(ByteBuf bb) {
    return new StreamId(Varint.read(bb));
  }

  private final Varint id;

  public StreamId(final Varint id) {
    this.id = id;
  }

  public StreamId(final long id) {
    this.id = new Varint(id);
  }

  public void write(ByteBuf bb) {
    id.write(bb);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final StreamId streamId = (StreamId) o;

    return id == streamId.id;

  }

  @Override
  public int hashCode() {
    return (int) (id.getValue() ^ (id.getValue() >>> 32));
  }
}
