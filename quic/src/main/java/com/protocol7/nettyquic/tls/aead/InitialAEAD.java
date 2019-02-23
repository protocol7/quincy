package com.protocol7.nettyquic.tls.aead;

import static com.protocol7.nettyquic.tls.aead.Labels.*;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.tls.HKDF;
import com.protocol7.nettyquic.utils.Hex;

public class InitialAEAD {

  private static final byte[] QUIC_VERSION_1_SALT =
      Hex.dehex("ef4fb0abb47470c41befcf8031334fae485e09a0");

  public static AEAD create(ConnectionId connId, boolean isClient) {
    byte[] initialSecret = HKDF.extract(QUIC_VERSION_1_SALT, connId.asBytes());

    int length = 32;

    byte[] clientSecret = expand(initialSecret, CLIENT_INITIAL, length);
    byte[] serverSecret = expand(initialSecret, SERVER_INITIAL, length);

    byte[] mySecret;
    byte[] otherSecret;
    if (isClient) {
      mySecret = clientSecret;
      otherSecret = serverSecret;
    } else {
      mySecret = serverSecret;
      otherSecret = clientSecret;
    }

    byte[] myKey = expand(mySecret, KEY, 16);
    byte[] myIV = expand(mySecret, IV, 12);

    byte[] otherKey = expand(otherSecret, KEY, 16);
    byte[] otherIV = expand(otherSecret, IV, 12);

    byte[] myPnKey = expand(mySecret, HP_KEY, 16);
    byte[] otherPnKey = expand(otherSecret, HP_KEY, 16);

    return new AEAD(myKey, otherKey, myIV, otherIV, myPnKey, otherPnKey);
  }

  private static byte[] expand(byte[] secret, String label, int length) {
    return HKDF.expandLabel(secret, label, new byte[0], length);
  }
}
