package com.protocol7.nettyquick.tls.aead;

import static com.protocol7.nettyquick.utils.Hex.hex;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.utils.Hex;
import org.junit.Test;

public class OneRttAEADTest {

  @Test
  public void knownTls() {
    byte[] handshakeSecret =
        Hex.dehex("fb9fc80689b3a5d02c33243bf69a1b1b20705588a794304a6e7120155edf149a");
    byte[] handshakeHash =
        Hex.dehex("22844b930e5e0a59a09d5ac35fc032fc91163b193874a265236e568077378d8b");

    AEAD aead = OneRttAEAD.create(handshakeSecret, handshakeHash, false, true);

    assertEquals("49134b95328f279f0183860589ac6707", hex(aead.getMyKey()));
    assertEquals("0b6d22c8ff68097ea871c672073773bf", hex(aead.getOtherKey()));
    assertEquals("bc4dd5f7b98acff85466261d", hex(aead.getMyIV()));
    assertEquals("1b13dd9f8d8f17091d34b349", hex(aead.getOtherIV()));
  }

  @Test
  public void knownQuic() {
    byte[] handshakeSecret =
        Hex.dehex("54beccbbf7307051b43b5a99b4f58face05bf67e9e1cd3e0ac7e3cd04f9f0f8e");
    byte[] handshakeHash =
        Hex.dehex("53c9436f0219891b0f4be411dfa186b5db4c498c256454184005040e7bf3ee60");

    AEAD aead = OneRttAEAD.create(handshakeSecret, handshakeHash, true, true);

    assertEquals("3817066552b0555ac9f19fd03aaa1a75", hex(aead.getMyKey()));
    assertEquals("92ce8ebfde01e1809ca645eda4531425", hex(aead.getOtherKey()));
    assertEquals("47a328940d70e590f9dba411", hex(aead.getMyIV()));
    assertEquals("f6e3224e9709c000034dbab7", hex(aead.getOtherIV()));
  }
}
