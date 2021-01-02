package com.protocol7.quincy.tls.messages;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.tls.extensions.SupportedVersions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class EncryptedExtensionsTest {

  @Test
  public void roundtrip() {
    final EncryptedExtensions ee = new EncryptedExtensions(SupportedVersions.TLS13);

    final ByteBuf bb = Unpooled.buffer();
    ee.write(bb);

    final EncryptedExtensions parsedEE = EncryptedExtensions.parse(bb, true);

    assertEquals(ee.getExtensions(), parsedEE.getExtensions());
  }
}
