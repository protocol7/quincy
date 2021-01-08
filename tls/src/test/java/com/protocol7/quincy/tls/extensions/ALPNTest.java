package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ALPNTest {

  @Test
  public void roundtrip() {
    final ALPN alpn = new ALPN(ALPN.from("h3"));

    final ByteBuf bb = Unpooled.buffer();

    alpn.write(bb, false);

    final ALPN parsed = ALPN.parse(bb);

    assertArrayEquals(alpn.getProtocols(), parsed.getProtocols());
  }

  @Test
  public void contains() {
    final ALPN alpn = new ALPN(ALPN.from("h3", "http/0.9"));

    assertTrue(alpn.contains("h3".getBytes()));
    assertTrue(alpn.contains("http/0.9".getBytes()));
    assertFalse(alpn.contains("http/1.1".getBytes()));
  }
}
