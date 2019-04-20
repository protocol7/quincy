package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ServerNameTest {

  @Test
  public void roundtrip() {
    final ServerName serverName = new ServerName("hello.example.org");

    final ByteBuf bb = Unpooled.buffer();

    serverName.write(bb, false);

    final ServerName parsed = ServerName.parse(bb);

    assertEquals(serverName, parsed);
    assertEquals("hello.example.org", parsed.getServerName());
  }
}
