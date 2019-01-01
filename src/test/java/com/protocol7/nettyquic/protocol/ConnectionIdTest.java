package com.protocol7.nettyquic.protocol;

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
      assertTrue(connId.getLength() <= 15);
    }
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
}
