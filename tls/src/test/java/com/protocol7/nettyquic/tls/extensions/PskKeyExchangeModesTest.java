package com.protocol7.nettyquic.tls.extensions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PskKeyExchangeModesTest {

  @Test
  public void roundtrip() {
    PskKeyExchangeModes ext = new PskKeyExchangeModes(0x01);

    ByteBuf bb = Unpooled.buffer();
    ext.write(bb, true);

    PskKeyExchangeModes parsed = PskKeyExchangeModes.parse(bb);

    assertEquals(ext, parsed);
  }
}
