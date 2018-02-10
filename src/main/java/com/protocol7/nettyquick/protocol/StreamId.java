package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.utils.Rnd;

public class StreamId {

  public static StreamId create() {
    return new StreamId(Rnd.rndLong());
  }

  private final long id;

  public StreamId(final long id) {
    this.id = id;
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
    return (int) (id ^ (id >>> 32));
  }
}
