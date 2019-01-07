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

    AEAD aead = HandshakeAEAD.create(handshakeSecret, helloHash, false, true);

    assertEquals("7154f314e6be7dc008df2c832baa1d39", hex(aead.getMyKey()));
    assertEquals("844780a7acad9f980fa25c114e43402a", hex(aead.getOtherKey()));
    assertEquals("71abc2cae4c699d47c600268", hex(aead.getMyIV()));
    assertEquals("4c042ddc120a38d1417fc815", hex(aead.getOtherIV()));
  }
}
