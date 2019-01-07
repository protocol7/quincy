package com.protocol7.nettyquic.tls.aead;

import com.protocol7.nettyquic.tls.HKDF;

public class HandshakeAEAD {

  public static AEAD create(
      byte[] handshakeSecret, byte[] helloHash, boolean quic, boolean isClient) {
    String labelPrefix = getPrefix(quic);

    // client_handshake_traffic_secret = hkdf-Expand-Label(
    //    key = handshake_secret,
    //    label = "c hs traffic",
    //    context = hello_hash,
    //    len = 32)
    byte[] clientHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, "tls13 ", "c hs traffic", helloHash, 32);

    // server_handshake_traffic_secret = hkdf-Expand-Label(
    //    key = handshake_secret,
    //    label = "s hs traffic",
    //    context = hello_hash,
    //    len = 32)
    byte[] serverHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, "tls13 ", "s hs traffic", helloHash, 32);

    // client_handshake_key = hkdf-Expand-Label(
    //    key = client_handshake_traffic_secret,
    //    label = "key",
    //    context = "",
    //    len = 16)
    byte[] clientHandshakeKey =
        HKDF.expandLabel(clientHandshakeTrafficSecret, labelPrefix, "key", new byte[0], 16);

    // server_handshake_key = hkdf-Expand-Label(
    //    key = server_handshake_traffic_secret,
    //    label = "key",
    //    context = "",
    //    len = 16)
    byte[] serverHandshakeKey =
        HKDF.expandLabel(serverHandshakeTrafficSecret, labelPrefix, "key", new byte[0], 16);

    // client_handshake_iv = hkdf-Expand-Label(
    //    key = client_handshake_traffic_secret,
    //    label = "iv",
    //    context = "",
    //    len = 12)
    byte[] clientHandshakeIV =
        HKDF.expandLabel(clientHandshakeTrafficSecret, labelPrefix, "iv", new byte[0], 12);

    // server_handshake_iv = hkdf-Expand-Label(
    //    key = server_handshake_traffic_secret,
    //    label = "iv",
    //    context = "",
    //    len = 12)
    byte[] serverHandshakeIV =
        HKDF.expandLabel(serverHandshakeTrafficSecret, labelPrefix, "iv", new byte[0], 12);

    byte[] clientPnKey =
        HKDF.expandLabel(clientHandshakeTrafficSecret, labelPrefix, "hp", new byte[0], 16);
    byte[] serverPnKey =
        HKDF.expandLabel(serverHandshakeTrafficSecret, labelPrefix, "hp", new byte[0], 16);

    if (isClient) {
      return new AEAD(
          clientHandshakeKey,
          serverHandshakeKey,
          clientHandshakeIV,
          serverHandshakeIV,
          clientPnKey,
          serverPnKey);
    } else {
      return new AEAD(
          serverHandshakeKey,
          clientHandshakeKey,
          serverHandshakeIV,
          clientHandshakeIV,
          serverPnKey,
          clientPnKey);
    }
  }

  private static String getPrefix(boolean quic) {
    String labelPrefix;
    if (quic) {
      labelPrefix = HKDF.QUIC_LABEL_PREFIX;
    } else {
      labelPrefix = HKDF.TLS_13_LABEL_PREFIX;
    }
    return labelPrefix;
  }
}
