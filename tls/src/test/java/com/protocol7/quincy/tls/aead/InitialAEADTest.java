package com.protocol7.quincy.tls.aead;

import com.protocol7.quincy.utils.Hex;
import org.junit.Assert;
import org.junit.Test;

public class InitialAEADTest {

  @Test
  public void testSecretsGenerator() {
    final byte[] connId = Hex.dehex("8394c8f03e515708"); // from RFC

    final AEAD aead = InitialAEAD.create(connId, true);

    Assert.assertEquals("98b0d7e5e7a402c67c33f350fa65ea54", Hex.hex(aead.getMyKey()));
    Assert.assertEquals("9a8be902a9bdd91d16064ca118045fb4", Hex.hex(aead.getOtherKey()));
    Assert.assertEquals("19e94387805eb0b46c03a788", Hex.hex(aead.getMyIV()));
    Assert.assertEquals("0a82086d32205ba22241d8dc", Hex.hex(aead.getOtherIV()));
    Assert.assertEquals("0edd982a6ac527f2eddcbb7348dea5d7", Hex.hex(aead.getMyPnKey()));
    Assert.assertEquals("94b9452d2b3c7c7f6da7fdd8593537fd", Hex.hex(aead.getOtherPnKey()));
  }

  @Test
  public void testSpecExample() {
    final byte[] connId = Hex.dehex("8394c8f03e515708"); // from RFC

    final AEAD aead = InitialAEAD.create(connId, true);

    Assert.assertEquals("1f369613dd76d5467730efcbe3b1a22d", Hex.hex(aead.getMyKey()));
    Assert.assertEquals("cf3a5331653c364c88f0f379b6067e37", Hex.hex(aead.getOtherKey()));
    Assert.assertEquals("fa044b2f42a3fd3b46fb255c", Hex.hex(aead.getMyIV()));
    Assert.assertEquals("0ac1493ca1905853b0bba03e", Hex.hex(aead.getOtherIV()));
    Assert.assertEquals("9f50449e04a0e810283a1e9933adedd2", Hex.hex(aead.getMyPnKey()));
    Assert.assertEquals("c206b8d9b9f0f37644430b490eeaa314", Hex.hex(aead.getOtherPnKey()));
  }
}
