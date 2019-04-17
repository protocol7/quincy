package com.protocol7.quincy.tls.extensions;

import static com.protocol7.quincy.tls.extensions.SupportedVersions.TLS13;
import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;

public class SupportedVersionsTest {

  @Test
  public void roundtripClientToServer() {
    assertRoundtrip(true);
  }

  @Test
  public void roundtripServerToClient() {
    assertRoundtrip(false);
  }

  private void assertRoundtrip(boolean clientToServer) {
    ByteBuf bb = Unpooled.buffer();

    TLS13.write(bb, clientToServer);

    SupportedVersions parsed = SupportedVersions.parse(bb, !clientToServer);
    assertEquals(List.of(SupportedVersion.TLS13), parsed.getVersions());
  }
}
