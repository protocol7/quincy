package com.protocol7.nettyquic.protocol;

import static com.google.common.base.Preconditions.checkArgument;

import com.protocol7.nettyquic.utils.Hex;
import com.protocol7.nettyquic.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Optional;

public class ConnectionId {

  private static final int MIN_LENGTH = 4;
  private static final int MAX_LENGTH = 18;

  public static ConnectionId random() {
    final byte[] id = new byte[Rnd.rndInt(MIN_LENGTH, MAX_LENGTH)];
    Rnd.rndBytes(id);
    return new ConnectionId(id);
  }

  public static ConnectionId read(final int length, final ByteBuf bb) {
    final byte[] id = new byte[length];
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

  public static int firstLength(final int cil) {
    int l = ((cil & 0b11110000) >> 4);
    if (l > 0) {
      return l + 3;
    } else {
      return 0;
    }
  }

  public static int lastLength(final int cil) {
    int l = ((cil & 0b00001111));
    if (l > 0) {
      return l + 3;
    } else {
      return 0;
    }
  }

  public static int joinLenghts(
      final Optional<ConnectionId> dcid, final Optional<ConnectionId> scid) {
    final int dcil = dcid.map(id -> id.getLength() - 3).orElse(0);
    final int scil = scid.map(id -> id.getLength() - 3).orElse(0);
    return (dcil << 4 | scil) & 0xFF;
  }

  private final byte[] id;

  public ConnectionId(final byte[] id) {
    checkArgument(id.length >= MIN_LENGTH);
    checkArgument(id.length <= MAX_LENGTH);

    this.id = id;
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(id);
  }

  public int getLength() {
    return id.length;
  }

  public byte[] asBytes() {
    return id;
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
    return Arrays.hashCode(id);
  }

  @Override
  public String toString() {
    return Hex.hex(id);
  }
}
