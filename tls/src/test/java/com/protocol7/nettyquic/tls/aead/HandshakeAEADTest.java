package com.protocol7.nettyquic.tls.aead;

import static com.protocol7.nettyquic.utils.Hex.dehex;
import static com.protocol7.nettyquic.utils.Hex.hex;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.tls.HKDF;
import org.junit.Test;

public class HandshakeAEADTest {

  @Test
  public void testKnownTls() {
    byte[] sharedSecret = dehex("df4a291baa1eb7cfa6934b29b474baad2697e29f1f920dcc77c8a0a088447624");
    byte[] helloHash = dehex("da75ce1139ac80dae4044da932350cf65c97ccc9e33f1e6f7d2d4b18b736ffd5");

    byte[] handshakeSecret = HKDF.calculateHandshakeSecret(sharedSecret);

    AEAD aead = HandshakeAEAD.create(handshakeSecret, helloHash, true);

    assertEquals("25f3dd7e6173c9e01647cb9ef2f71c3d", hex(aead.getMyKey()));
    assertEquals("7832ada4194357104157f645758e34fb", hex(aead.getOtherKey()));
    assertEquals("034c31fe01a40f3734ae1420", hex(aead.getMyIV()));
    assertEquals("c6a739c3e2d30f92e89a9289", hex(aead.getOtherIV()));
  }
}
