package com.protocol7.nettyquick.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConnectionIdTest {

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