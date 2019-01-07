package com.protocol7.nettyquic.tls.aead;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.tls.HKDF;
import com.protocol7.nettyquic.utils.Hex;

public class NullAEAD {

  private static final byte[] QUIC_VERSION_1_SALT =
      Hex.dehex("ef4fb0abb47470c41befcf8031334fae485e09a0");

  public static AEAD create(ConnectionId connId, boolean isClient) {
    byte[] initialSecret = HKDF.extract(QUIC_VERSION_1_SALT, connId.asBytes());

    int length = 32;

    byte[] clientSecret = expand(initialSecret, "client in", length);
    byte[] serverSecret = expand(initialSecret, "server in", length);

    byte[] mySecret;
    byte[] otherSecret;
    if (isClient) {
      mySecret = clientSecret;
      otherSecret = serverSecret;
    } else {
      mySecret = serverSecret;
      otherSecret = clientSecret;
    }

    byte[] myKey = expand(mySecret, "quic key", 16);
    byte[] myIV = expand(mySecret, "quic iv", 12);

    byte[] otherKey = expand(otherSecret, "quic key", 16);
    byte[] otherIV = expand(otherSecret, "quic iv", 12);

    byte[] myPnKey = expand(mySecret, "quic hp", 16);
    byte[] otherPnKey = expand(otherSecret, "quic hp", 16);

    return new AEAD(myKey, otherKey, myIV, otherIV, myPnKey, otherPnKey);
  }

  private static byte[] expand(byte[] secret, String label, int length) {
    return HKDF.expandLabel(secret, label, new byte[0], length);
  }
}
