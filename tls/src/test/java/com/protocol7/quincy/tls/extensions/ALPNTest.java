package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ALPNTest {

  @Test
  public void roundtrip() {
    final ALPN alpn = new ALPN("h3");

    final ByteBuf bb = Unpooled.buffer();

    alpn.write(bb, false);

    final ALPN parsed = ALPN.parse(bb);

    assertEquals(alpn.getProtocols(), parsed.getProtocols());
  }
}
