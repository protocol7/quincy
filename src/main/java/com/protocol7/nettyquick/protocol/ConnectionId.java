package com.protocol7.nettyquick.protocol;

import com.google.common.primitives.Longs;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;

public class ConnectionId {

  public static ConnectionId random() {
    return new ConnectionId(Rnd.rndLong());
  }

  public static ConnectionId read(final ByteBuf bb) {
    return new ConnectionId(bb.readLong());
  }

  private final long id;

  public ConnectionId(final long id) {
    this.id = id;
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(Longs.toByteArray(id));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ConnectionId that = (ConnectionId) o;

    return id == that.id;
  }

  @Override
  public int hashCode() {
    return (int) (id ^ (id >>> 32));
  }

  @Override
  public String toString() {
    return Long.toString(id);
  }
}
