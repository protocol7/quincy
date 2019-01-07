package com.protocol7.nettyquic.tls.aead;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.utils.Hex;
import org.junit.Assert;
import org.junit.Test;

public class NullAEADTest {

  @Test
  public void testSecretsGenerator() {
    ConnectionId connId = new ConnectionId(Hex.dehex("c574992996084bbe8e02b3cbb91fdd"));

    AEAD aead = NullAEAD.create(connId, true);

    Assert.assertEquals("c3d413881123d5ba92583c5beb798ecf", Hex.hex(aead.getMyKey()));
    Assert.assertEquals("bfa010b9339b3f09221521169d826362", Hex.hex(aead.getOtherKey()));
    Assert.assertEquals("d0844438e77ac16643d4080a", Hex.hex(aead.getMyIV()));
    Assert.assertEquals("f7daa3324ffce70f7c4cce9b", Hex.hex(aead.getOtherIV()));
    Assert.assertEquals("66208e3ab041aebdf22fc1d675febba8", Hex.hex(aead.getMyPnKey()));
    Assert.assertEquals("b69a4e13cf96e0c6cf50474cc13beed6", Hex.hex(aead.getOtherPnKey()));
  }
}
