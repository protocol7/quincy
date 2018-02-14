package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ConnectionIdTest {

  @Test
  public void roundtrip() {
    ConnectionId connectionId = ConnectionId.random();
    ByteBuf bb = Unpooled.buffer();

    connectionId.write(bb);

    ConnectionId parsed = ConnectionId.read(bb);

    assertEquals(parsed, connectionId);
  }

}