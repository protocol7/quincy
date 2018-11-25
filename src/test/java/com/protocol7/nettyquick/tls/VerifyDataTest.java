package com.protocol7.nettyquick.tls;

import static com.protocol7.nettyquick.TestUtil.assertHex;
import static com.protocol7.nettyquick.tls.VerifyData.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Rnd;
import org.junit.Test;

public class VerifyDataTest {

  private final byte[] handshakeTrafficSecret =
      Hex.dehex("eb40b3b31cd6fc0ab7f2cdac01f4e91b9aebb729d76d63ee60f043a0daac12a6");
  private final byte[] finishedHash =
      Hex.dehex("7a795a4b9ee1753b2dcdbfe3d7718e5b7a6b85b8c5b17239543d9ea8828449c7");

  private final byte[] tlsVD =
      Hex.dehex("447cdde9e7321b90305f97755a1dcf630b9e0d03e3d5a0b9f99434eb677f319d");
  private final byte[] quicVD =
      Hex.dehex("1e21c0b40fe80e17214e1774d889b8278d908b80ff57ec88da657903d6295b44");

  @Test
  public void testCreate() {
    assertHex(tlsVD, create(handshakeTrafficSecret, finishedHash, false));
    assertHex(quicVD, create(handshakeTrafficSecret, finishedHash, true));
  }

  @Test
  public void testVerify() {
    assertTrue(VerifyData.verify(tlsVD, handshakeTrafficSecret, finishedHash, false));
    assertTrue(VerifyData.verify(quicVD, handshakeTrafficSecret, finishedHash, true));

    assertFalse(VerifyData.verify(Rnd.rndBytes(32), handshakeTrafficSecret, finishedHash, false));
  }
}
