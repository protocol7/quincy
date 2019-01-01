package com.protocol7.nettyquic.tls.extensions;

import static com.protocol7.nettyquic.tls.extensions.SupportedVersions.TLS13;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class SupportedVersionsTest {

  @Test
  public void roundtrip() {
    ByteBuf bb = Unpooled.buffer();

    TLS13.write(bb, true);

    SupportedVersions parsed = SupportedVersions.parse(bb, true);
    assertEquals(Hex.hex(TLS13.getVersion()), Hex.hex(parsed.getVersion()));
  }
}
