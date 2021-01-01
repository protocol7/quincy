package com.protocol7.quincy.protocol;

import static com.google.common.base.Preconditions.checkArgument;

import com.protocol7.quincy.utils.Hex;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;

public class ConnectionId {

  private static final int MIN_LENGTH = 0;
  public static final int LENGTH = 18;
  public static final int MAX_LENGTH = 20;

  public static final ConnectionId EMPTY = new ConnectionId(new byte[0]);

  public static ConnectionId random() {
    final byte[] id = new byte[LENGTH];
    Rnd.rndBytes(id);
    return new ConnectionId(id);
  }

  public static ConnectionId read(final int length, final ByteBuf bb) {
    if (length > 0) {
      final byte[] id = new byte[length];
      bb.readBytes(id);
      return new ConnectionId(id);
    } else {
      return ConnectionId.EMPTY;
    }
  }

  /** Read connection id prefixed by its length from buffer */
  public static ConnectionId read(final ByteBuf bb) {
    final int len = bb.readByte() & 0xFF;
    return read(len, bb);
  }

  /**
   * Write connection ID prefixed by its length to buffer. Write 0 length if connection ID not
   * available.
   */
  public static void write(final ConnectionId connectionId, final ByteBuf bb) {
    bb.writeByte(connectionId.getLength());
    connectionId.write(bb);
  }

  private final byte[] id;

  public ConnectionId(final byte[] id) {
    checkArgument(id.length >= MIN_LENGTH, "Connection ID too short: " + id.length);
    checkArgument(id.length <= MAX_LENGTH, "Connection ID too long: " + id.length);

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

  public ByteBuf asByteBuffer() {
    return Unpooled.wrappedBuffer(id);
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
