package com.protocol7.nettyquick.protocol;

import static com.protocol7.nettyquick.utils.Bits.set;
import static com.protocol7.nettyquick.utils.Bits.unset;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class StreamId {

  public static StreamId random(boolean client, boolean bidirectional) {
    long id = Varint.random(1);

    id = encodeType(client, bidirectional, id);

    return new StreamId(id);
  }

  private static long encodeType(final boolean client, final boolean bidirectional, long id) {
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
    return id;
  }

  public static StreamId next(StreamId prev, boolean client, boolean bidirectional) {
    long v = prev.getValue();

    v = encodeType(client, bidirectional, v);

    long tmp = v;
    while (v <= prev.getValue()) {
      tmp++;
      v = encodeType(client, bidirectional, tmp);
    }

    return new StreamId(v);
  }

  public static StreamId parse(ByteBuf bb) {
    return new StreamId(Varint.readAsLong(bb));
  }

  private final long id;

  public StreamId(final long id) {
    Preconditions.checkArgument(id >= 0);
    Preconditions.checkArgument(id <= Varint.MAX);

    this.id = id;
  }

  public boolean isClient() {
    return (id & 1) == 0;
  }

  public boolean isBidirectional() {
    return (id & 0b10) == 0;
  }

  public void write(ByteBuf bb) {
    Varint.write(id, bb);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StreamId streamId = (StreamId) o;
    return id == streamId.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "StreamId{" + id + '}';
  }

  public long getValue() {
    return id;
  }
}
