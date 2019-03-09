package com.protocol7.nettyquic.tls.aead;

import static com.protocol7.nettyquic.utils.Hex.hex;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.utils.Hex;
import org.junit.Test;

public class OneRttAEADTest {

  @Test
  public void knownTls() {
    byte[] handshakeSecret =
        Hex.dehex("fb9fc80689b3a5d02c33243bf69a1b1b20705588a794304a6e7120155edf149a");
    byte[] handshakeHash =
        Hex.dehex("22844b930e5e0a59a09d5ac35fc032fc91163b193874a265236e568077378d8b");

    AEAD aead = OneRttAEAD.create(handshakeSecret, handshakeHash, true);

    assertEquals("3eb3fe82f5ac8e55458068f6f09a0e07", hex(aead.getMyKey()));
    assertEquals("e285f91ba60eae359d767af707710e45", hex(aead.getOtherKey()));
    assertEquals("fb51b454f6e2d176ae835d77", hex(aead.getMyIV()));
    assertEquals("1f3f0add9b67d2c388143e44", hex(aead.getOtherIV()));
  }
}
