package com.protocol7.nettyquick.protocol;

import static com.protocol7.nettyquick.utils.Bits.set;
import static com.protocol7.nettyquick.utils.Bits.unset;

import io.netty.buffer.ByteBuf;

public class StreamId {

  public static StreamId random(boolean client, boolean bidirectional) {
    long id = Varint.random(1).getValue();

    if (client) {
      id = unset(id, 0);
    } else {
      id = set(id, 0);
    }

    if (bidirectional) {
      id = unset(id, 1);
    } else {
      id = set(id, 1);
    }

    return new StreamId(id);
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

  public boolean isClient() {
    return (id.getValue() & 1) == 0;
  }

  public boolean isBidirectional() {
    return (id.getValue() & 0b10) == 0;
  }

  public void write(ByteBuf bb) {
    id.write(bb);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final StreamId streamId = (StreamId) o;
    return id.equals(streamId.id);

  }

  @Override
  public int hashCode() {
    return (int) (id.getValue() ^ (id.getValue() >>> 32));
  }

  @Override
  public String toString() {
    return id.toString();
  }

  public long getValue() {
    return id.getValue();
  }
}
