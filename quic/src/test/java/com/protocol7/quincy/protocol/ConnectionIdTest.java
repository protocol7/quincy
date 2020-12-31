package com.protocol7.quincy.protocol;

import static java.util.Optional.of;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;

public class ConnectionIdTest {

  @Test
  public void randomLength() {
    for (int i = 0; i < 1000; i++) {
      final ConnectionId connId = ConnectionId.random();
      assertTrue(connId.getLength() >= 8);
      assertTrue(connId.getLength() <= 20);
    }
  }

  @Test
  public void cstrLengths() {
    new ConnectionId(new byte[4]);
    new ConnectionId(new byte[18]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cstrLengthsTooShort() {
    new ConnectionId(new byte[3]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cstrLengthsTooLong() {
    new ConnectionId(new byte[21]);
  }

  @Test
  public void roundtrip() {
    final ByteBuf bb = Unpooled.buffer();
    final ConnectionId connectionId = ConnectionId.random();

    connectionId.write(bb);

    final ConnectionId parsed = ConnectionId.read(connectionId.getLength(), bb).get();

    assertEquals(parsed, connectionId);
  }

  @Test
  public void roundtripWithLenth() {
    final ByteBuf bb = Unpooled.buffer();
    final ConnectionId connectionId = ConnectionId.random();

    ConnectionId.write(Optional.of(connectionId), bb);

    final ConnectionId parsed = ConnectionId.read(bb).get();

    assertEquals(parsed, connectionId);
  }

  @Test
  public void roundtripEmptyWithLenth() {
    final ByteBuf bb = Unpooled.buffer();

    ConnectionId.write(Optional.empty(), bb);

    assertEquals(Optional.empty(), ConnectionId.read(bb));
  }

  @Test
  public void writeIds() {
    final byte[] b16 = new byte[16];
    final ConnectionId id16 = new ConnectionId(b16);
    final byte[] b18 = new byte[18];
    Arrays.fill(b18, (byte) 1);
    final ConnectionId id18 = new ConnectionId(b18);

    final ByteBuf bb = Unpooled.buffer();
    ConnectionId.write(of(id16), bb);

    assertEquals(b16.length, bb.readByte() & 0xFF);
    final byte[] a16 = new byte[16];
    bb.readBytes(a16);
    assertArrayEquals(b16, a16);
  }

  @Test
  public void read() {
    final ByteBuf bb = Unpooled.buffer();
    final byte[] b16 = new byte[16];
    bb.writeByte(b16.length);
    bb.writeBytes(b16);

    assertArrayEquals(b16, ConnectionId.read(bb).get().asBytes());
  }
}
