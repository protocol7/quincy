package com.protocol7.quincy.tls.aead;

import com.protocol7.quincy.tls.ConstantTimeEquals;
import com.protocol7.quincy.utils.Hex;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class RetryTokenIntegrityTagAEAD {

  private static final byte[] SECRET = Hex.dehex("ccce187ed09a09d05728155a6cb96be1");
  private static final byte[] NONCE = Hex.dehex("e54930f97f2136f0530a8c1c");

  public byte[] create(final byte[] retryPseudoPacket) throws GeneralSecurityException {
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
    final SecretKey secretKey = new SecretKeySpec(SECRET, 0, SECRET.length, "AES");
    final GCMParameterSpec spec = new GCMParameterSpec(128, NONCE);

    cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
    cipher.updateAAD(retryPseudoPacket);
    return cipher.doFinal(new byte[0]);
  }

  public void verify(final byte[] retryPseudoPacket, final byte[] tag)
      throws GeneralSecurityException {
    final byte[] actualTag = create(retryPseudoPacket);

    if (!ConstantTimeEquals.isEqual(tag, actualTag)) {
      throw new RuntimeException("Invalid retry token integrity tag");
    }
  }
}
