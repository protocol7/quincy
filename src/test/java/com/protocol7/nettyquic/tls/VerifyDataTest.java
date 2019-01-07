package com.protocol7.nettyquic.tls;

import static com.protocol7.nettyquic.TestUtil.assertHex;
import static com.protocol7.nettyquic.tls.VerifyData.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquic.utils.Hex;
import com.protocol7.nettyquic.utils.Rnd;
import org.junit.Test;

public class VerifyDataTest {

  private final byte[] handshakeTrafficSecret =
      Hex.dehex("eb40b3b31cd6fc0ab7f2cdac01f4e91b9aebb729d76d63ee60f043a0daac12a6");
  private final byte[] finishedHash =
      Hex.dehex("7a795a4b9ee1753b2dcdbfe3d7718e5b7a6b85b8c5b17239543d9ea8828449c7");

  private final byte[] tlsVD =
      Hex.dehex("447cdde9e7321b90305f97755a1dcf630b9e0d03e3d5a0b9f99434eb677f319d");

  @Test
  public void testCreate() {
    assertHex(tlsVD, create(handshakeTrafficSecret, finishedHash, false));
  }

  @Test
  public void testVerify() {
    assertTrue(VerifyData.verify(tlsVD, handshakeTrafficSecret, finishedHash, false));

    assertFalse(VerifyData.verify(Rnd.rndBytes(32), handshakeTrafficSecret, finishedHash, false));
  }
}
