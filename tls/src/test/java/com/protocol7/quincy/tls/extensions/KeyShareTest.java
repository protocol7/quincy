package com.protocol7.quincy.tls.extensions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.tls.Group;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class KeyShareTest {

  private byte[] key = Rnd.rndBytes(32);
  private byte[] key2 = Rnd.rndBytes(32);
  private KeyShare ext = KeyShare.of(Group.X25519, key);
  private KeyShare extMultiple = KeyShare.of(Group.X25519, key, Group.X448, key2);

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

  private void assertRoundtripSingle(boolean clientToServer) {
    ByteBuf bb = Unpooled.buffer();

    ext.write(bb, clientToServer);

    KeyShare parsed = KeyShare.parse(bb, !clientToServer);

    assertEquals(ext.getKeys().size(), parsed.getKeys().size());
    assertArrayEquals(ext.getKey(Group.X25519).get(), parsed.getKey(Group.X25519).get());
  }

  @Test
  public void roundtripMultiple() {
    ByteBuf bb = Unpooled.buffer();

    extMultiple.write(bb, true);

    KeyShare parsed = KeyShare.parse(bb, false);

    assertEquals(extMultiple.getKeys().size(), parsed.getKeys().size());
    assertArrayEquals(extMultiple.getKey(Group.X25519).get(), parsed.getKey(Group.X25519).get());
    assertArrayEquals(extMultiple.getKey(Group.X448).get(), parsed.getKey(Group.X448).get());
  }
}
