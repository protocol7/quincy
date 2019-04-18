package com.protocol7.quincy.protocol;

import static com.protocol7.quincy.utils.Bits.set;
import static com.protocol7.quincy.utils.Bits.unset;

import com.google.common.base.Preconditions;
import com.protocol7.quincy.Varint;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class StreamId {

  public static StreamId random(final boolean client, final boolean bidirectional) {
    long id = Varint.random(4);
    id = encodeType(client, bidirectional, id);

    return new StreamId(id);
  }

  private static long encodeType(final boolean client, final boolean bidirectional, final long id) {
    long res = id;
    if (client) {
      res = unset(res, 0);
    } else {
      res = set(res, 0);
    }
    if (bidirectional) {
      res = unset(res, 1);
    } else {
      res = set(res, 1);
    }
    return res;
  }

  public static StreamId next(
      final StreamId prev, final boolean client, final boolean bidirectional) {
    long v = prev.getValue();

    v = encodeType(client, bidirectional, v);

    long tmp = v;
    while (v <= prev.getValue()) {
      tmp++;
      v = encodeType(client, bidirectional, tmp);
    }

    return new StreamId(v);
  }

  public static StreamId parse(final ByteBuf bb) {
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

  public void write(final ByteBuf bb) {
    Varint.write(id, bb);
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
