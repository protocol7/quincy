package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ALPNTest {

  @Test
  public void roundtrip() {
    final ALPN alpn = new ALPN("h3", "http/0.9");

    final ByteBuf bb = Unpooled.buffer();

    alpn.write(bb, false);

    final ALPN parsed = ALPN.parse(bb);

    assertEquals(alpn.getProtocols(), parsed.getProtocols());
  }

  @Test
  public void contains() {
    final ALPN alpn = new ALPN("h3", "http/0.9");

    assertTrue(alpn.contains("h3"));
    assertTrue(alpn.contains("http/0.9"));
    assertFalse(alpn.contains("http/1.1"));
  }
}
