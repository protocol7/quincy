package com.protocol7.quincy.protocol;

import static com.protocol7.quincy.utils.Bits.set;
import static com.protocol7.quincy.utils.Bits.unset;

import com.google.common.base.Preconditions;
import com.protocol7.quincy.Varint;
import io.netty.buffer.ByteBuf;

public class StreamId {

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

  public static long next(final long prev, final boolean client, final boolean bidirectional) {
    long v = encodeType(client, bidirectional, prev);

    long tmp = v;
    while (v <= prev) {
      tmp++;
      v = encodeType(client, bidirectional, tmp);
    }

    return v;
  }

  public static long parse(final ByteBuf bb) {
    return Varint.readAsLong(bb);
  }

  public static long validate(final long id) {
    Preconditions.checkArgument(id >= 0);
    Preconditions.checkArgument(id <= Varint.MAX);
    return id;
  }

  public static boolean isClient(final long id) {
    return (id & 1) == 0;
  }

  public static boolean isBidirectional(final long id) {
    return (id & 0b10) == 0;
  }

  public static void write(final ByteBuf bb, final long id) {
    Varint.write(id, bb);
  }

  private StreamId() {}
}
