package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.tls.Group;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class KeyShareTest {

  private final byte[] key = Rnd.rndBytes(32);
  private final byte[] key2 = Rnd.rndBytes(32);
  private final KeyShare ext = KeyShare.of(Group.X25519, key);
  private final KeyShare extMultiple = KeyShare.of(Group.X25519, key, Group.X448, key2);

  @Test
  public void getType() {
    assertEquals(ExtensionType.KEY_SHARE, ext.getType());
  }

  @Test
  public void roundtripSingleClientToServer() {
    assertRoundtripSingle(true);
  }

  @Test
  public void roundtripSingleServerToClient() {
    assertRoundtripSingle(false);
  }

  private void assertRoundtripSingle(final boolean clientToServer) {
    final ByteBuf bb = Unpooled.buffer();

    ext.write(bb, clientToServer);

    final KeyShare parsed = KeyShare.parse(bb, !clientToServer);

    assertEquals(ext, parsed);
  }

  @Test
  public void roundtripMultiple() {
    final ByteBuf bb = Unpooled.buffer();

    extMultiple.write(bb, true);

    final KeyShare parsed = KeyShare.parse(bb, false);

    assertEquals(extMultiple, parsed);
  }
}
