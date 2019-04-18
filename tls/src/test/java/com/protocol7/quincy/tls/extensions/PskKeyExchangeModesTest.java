package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PskKeyExchangeModesTest {

  @Test
  public void roundtrip() {
    final PskKeyExchangeModes ext = new PskKeyExchangeModes(0x01);

    final ByteBuf bb = Unpooled.buffer();
    ext.write(bb, true);

    final PskKeyExchangeModes parsed = PskKeyExchangeModes.parse(bb);

    assertEquals(ext, parsed);
  }
}
