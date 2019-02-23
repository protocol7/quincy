package com.protocol7.nettyquic.protocol;

import static java.util.Optional.of;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquic.utils.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;

public class ConnectionIdTest {

  @Test
  public void randomLength() {
    for (int i = 0; i < 1000; i++) {
      ConnectionId connId = ConnectionId.random();
      assertTrue(connId.getLength() >= 8);
      assertTrue(connId.getLength() <= 18);
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
    new ConnectionId(new byte[19]);
  }

  @Test
  public void roundtrip() {
    assertRoundtrip(ConnectionId.random());
  }

  private void assertRoundtrip(ConnectionId connectionId) {
    ByteBuf bb = Unpooled.buffer();
    connectionId.write(bb);

    ConnectionId parsed = ConnectionId.read(connectionId.getLength(), bb);

    assertEquals(parsed, connectionId);
  }

  @Test
  public void writeIds() {
    byte[] b16 = new byte[16];
    ConnectionId id16 = new ConnectionId(b16);
    byte[] b18 = new byte[18];
    Arrays.fill(b18, (byte) 1);
    ConnectionId id18 = new ConnectionId(b18);

    ByteBuf bb = Unpooled.buffer();
    ConnectionId.write(of(id16), of(id18), bb);
    assertEquals(0xdf, bb.readByte() & 0xFF);

    byte[] a16 = new byte[16];
    bb.readBytes(a16);
    assertArrayEquals(b16, a16);
    byte[] a18 = new byte[18];
    bb.readBytes(a18);
    assertArrayEquals(b18, a18);
  }

  @Test
  public void readPair() {
    ByteBuf bb = Unpooled.buffer();
    bb.writeByte(0xdf);
    byte[] b16 = new byte[16];
    bb.writeBytes(b16);
    byte[] b18 = new byte[18];
    Arrays.fill(b18, (byte) 1);
    bb.writeBytes(b18);

    Pair<Optional<ConnectionId>, Optional<ConnectionId>> cids = ConnectionId.readPair(bb);

    assertArrayEquals(b16, cids.getFirst().get().asBytes());
    assertArrayEquals(b18, cids.getSecond().get().asBytes());
  }
}
