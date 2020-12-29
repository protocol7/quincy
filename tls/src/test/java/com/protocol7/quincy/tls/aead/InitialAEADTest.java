package com.protocol7.quincy.tls.aead;

import com.protocol7.quincy.utils.Hex;
import org.junit.Assert;
import org.junit.Test;

public class InitialAEADTest {

  @Test
  public void testSpecExample() {
    final byte[] connId = Hex.dehex("8394c8f03e515708"); // from RFC

    final AEAD aead = InitialAEAD.create(connId, true);

    Assert.assertEquals("175257a31eb09dea9366d8bb79ad80ba", Hex.hex(aead.getMyKey()));
    Assert.assertEquals("149d0b1662ab871fbe63c49b5e655a5d", Hex.hex(aead.getOtherKey()));
    Assert.assertEquals("6b26114b9cba2b63a9e8dd4f", Hex.hex(aead.getMyIV()));
    Assert.assertEquals("bab2b12a4c76016ace47856d", Hex.hex(aead.getOtherIV()));
    Assert.assertEquals("9ddd12c994c0698b89374a9c077a3077", Hex.hex(aead.getMyPnKey()));
    Assert.assertEquals("c0c499a65a60024a18a250974ea01dfa", Hex.hex(aead.getOtherPnKey()));
  }
}
