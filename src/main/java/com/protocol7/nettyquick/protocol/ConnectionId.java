package com.protocol7.nettyquick.protocol;

import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedLong;
import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.Optional;

public class ConnectionId {

  public static ConnectionId random() {
    byte[] id = new byte[12]; // TODO what length to use?
    Rnd.rndBytes(id);
    return new ConnectionId(id);
  }

  public static ConnectionId read(final int length, final ByteBuf bb) {
    byte[] id = new byte[length];
    bb.readBytes(id);
    return new ConnectionId(id);
  }

  public static Optional<ConnectionId> readOptional(final int length, final ByteBuf bb) {
    if (length > 0) {
      return Optional.of(read(length, bb));
    } else {
      return Optional.empty();
    }
  }

  public static int firstLength(int cil) {
    int l = ((cil & 0b11110000) >> 4);
    if (l > 0) {
      return l + 3;
    } else {
      return 0;
    }
  }

  public static int lastLength(int cil) {
    int l = ((cil & 0b00001111));
    if (l > 0) {
      return l + 3;
    } else {
      return 0;
    }
  }

  public static int joinLenghts(Optional<ConnectionId> id1, Optional<ConnectionId> id2) {
    int dcil;
    if (id1.isPresent()) {
      dcil = (id1.get().getLength() & 0b1111) - 3;
    } else {
      dcil = 0;
    }
    int scil;
    if (id2.isPresent()) {
      scil = (id2.get().getLength() & 0b1111) - 3;
    } else {
      scil = 0;
    }
    return (dcil << 4 | scil) & 0xFF;
  }

  private final byte[] id;

  public ConnectionId(final byte[] id) {
    this.id = id;
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(id);
  }

  public int getLength() {
    return id.length;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ConnectionId that = (ConnectionId) o;

    return Arrays.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return Hex.hex(id);
  }
}
