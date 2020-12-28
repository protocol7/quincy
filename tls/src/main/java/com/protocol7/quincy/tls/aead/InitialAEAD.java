package com.protocol7.quincy.tls.aead;

import static com.protocol7.quincy.tls.aead.Labels.CLIENT_INITIAL;
import static com.protocol7.quincy.tls.aead.Labels.HP_KEY;
import static com.protocol7.quincy.tls.aead.Labels.IV;
import static com.protocol7.quincy.tls.aead.Labels.KEY;
import static com.protocol7.quincy.tls.aead.Labels.SERVER_INITIAL;

import com.protocol7.quincy.tls.HKDF;
import com.protocol7.quincy.utils.Hex;

public class InitialAEAD {

  private static final byte[] QUIC_VERSION_1_SALT =
      Hex.dehex("afbfec289993d24c9e9786f19c6111e04390a899");

  public static AEAD create(final byte[] keyMaterial, final boolean isClient) {
    final byte[] initialSecret = HKDF.extract(QUIC_VERSION_1_SALT, keyMaterial);

    final int length = 32;

    final byte[] clientSecret = expand(initialSecret, CLIENT_INITIAL, length);
    final byte[] serverSecret = expand(initialSecret, SERVER_INITIAL, length);

    final byte[] mySecret;
    final byte[] otherSecret;
    if (isClient) {
      mySecret = clientSecret;
      otherSecret = serverSecret;
    } else {
      mySecret = serverSecret;
      otherSecret = clientSecret;
    }

    final byte[] myKey = expand(mySecret, KEY, 16);
    final byte[] myIV = expand(mySecret, IV, 12);

    final byte[] otherKey = expand(otherSecret, KEY, 16);
    final byte[] otherIV = expand(otherSecret, IV, 12);

    final byte[] myPnKey = expand(mySecret, HP_KEY, 16);
    final byte[] otherPnKey = expand(otherSecret, HP_KEY, 16);

    return new AEAD(myKey, otherKey, myIV, otherIV, myPnKey, otherPnKey);
  }

  private static byte[] expand(final byte[] secret, final String label, final int length) {
    return HKDF.expandLabel(secret, label, new byte[0], length);
  }
}
