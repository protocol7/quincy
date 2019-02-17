package com.protocol7.nettyquic.protocol;

import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
  public void joinLenghts() {
    ConnectionId id16 = new ConnectionId(new byte[16]);
    ConnectionId id18 = new ConnectionId(new byte[18]);

    assertEquals(0b11011101, ConnectionId.joinLenghts(of(id16), of(id16)));
    assertEquals(0b11111111, ConnectionId.joinLenghts(of(id18), of(id18)));
  }
}
