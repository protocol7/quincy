package com.protocol7.nettyquic.tls.aead;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.utils.Hex;
import org.junit.Assert;
import org.junit.Test;

public class NullAEADTest {

  @Test
  public void testSecretsGenerator() {
    ConnectionId connId = new ConnectionId(Hex.dehex("8394c8f03e515708")); // from RFC

    AEAD aead = NullAEAD.create(connId, true);

    Assert.assertEquals("98b0d7e5e7a402c67c33f350fa65ea54", Hex.hex(aead.getMyKey()));
    Assert.assertEquals("9a8be902a9bdd91d16064ca118045fb4", Hex.hex(aead.getOtherKey()));
    Assert.assertEquals("19e94387805eb0b46c03a788", Hex.hex(aead.getMyIV()));
    Assert.assertEquals("0a82086d32205ba22241d8dc", Hex.hex(aead.getOtherIV()));
    Assert.assertEquals("0edd982a6ac527f2eddcbb7348dea5d7", Hex.hex(aead.getMyPnKey()));
    Assert.assertEquals("94b9452d2b3c7c7f6da7fdd8593537fd", Hex.hex(aead.getOtherPnKey()));
  }
}
