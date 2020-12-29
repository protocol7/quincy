package com.protocol7.quincy.tls.aead;

import static org.junit.Assert.*;

import com.protocol7.quincy.utils.Hex;
import java.security.GeneralSecurityException;
import org.junit.Test;

public class RetryTokenIntegrityTagAEADTest {

  private final RetryTokenIntegrityTagAEAD aead = new RetryTokenIntegrityTagAEAD();
  private final byte[] retryPseudoPacket = new byte[18];

  @Test
  public void testCreate() throws GeneralSecurityException {
    final byte[] tag = aead.create(retryPseudoPacket);

    assertEquals("5535b8e1839966ae4c9c7cbd72cb668b", Hex.hex(tag));
  }

  @Test
  public void testVerify() throws GeneralSecurityException {
    aead.verify(retryPseudoPacket, Hex.dehex("5535b8e1839966ae4c9c7cbd72cb668b"));
  }

  @Test(expected = RuntimeException.class)
  public void testVerifyInvalid() throws GeneralSecurityException {
    aead.verify(retryPseudoPacket, Hex.dehex("1234"));
  }
}
