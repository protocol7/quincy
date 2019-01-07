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

    assertEquals("49134b95328f279f0183860589ac6707", hex(aead.getMyKey()));
    assertEquals("0b6d22c8ff68097ea871c672073773bf", hex(aead.getOtherKey()));
    assertEquals("bc4dd5f7b98acff85466261d", hex(aead.getMyIV()));
    assertEquals("1b13dd9f8d8f17091d34b349", hex(aead.getOtherIV()));
  }
}
