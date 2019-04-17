package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class SignatureAlgorithmsTest {

  @Test
  public void roundtrip() {
    SignatureAlgorithms ext = new SignatureAlgorithms(0x0804, 0x0403);

    ByteBuf bb = Unpooled.buffer();
    ext.write(bb, true);

    SignatureAlgorithms parsed = SignatureAlgorithms.parse(bb);

    assertEquals(ext, parsed);
  }
}
