package com.protocol7.nettyquic.tls.aead;

import static com.protocol7.nettyquic.tls.aead.Labels.*;

import com.protocol7.nettyquic.tls.HKDF;

public class HandshakeAEAD {

  public static AEAD create(byte[] handshakeSecret, byte[] helloHash, boolean isClient) {

    // client_handshake_traffic_secret = hkdf-Expand-Label(
    //    key = handshake_secret,
    //    label = "c hs traffic",
    //    context = hello_hash,
    //    len = 32)
    byte[] clientHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, CLIENT_HANDSHAKE_TRAFFIC_SECRET, helloHash, 32);

    // server_handshake_traffic_secret = hkdf-Expand-Label(
    //    key = handshake_secret,
    //    label = "s hs traffic",
    //    context = hello_hash,
    //    len = 32)
    byte[] serverHandshakeTrafficSecret =
        HKDF.expandLabel(handshakeSecret, SERVER_HANDSHAKE_TRAFFIC_SECRET, helloHash, 32);

    // client_handshake_key = hkdf-Expand-Label(
    //    key = client_handshake_traffic_secret,
    //    label = "key",
    //    context = "",
    //    len = 16)
    byte[] clientHandshakeKey =
        HKDF.expandLabel(clientHandshakeTrafficSecret, KEY, new byte[0], 16);

    // server_handshake_key = hkdf-Expand-Label(
    //    key = server_handshake_traffic_secret,
    //    label = "key",
    //    context = "",
    //    len = 16)
    byte[] serverHandshakeKey =
        HKDF.expandLabel(serverHandshakeTrafficSecret, KEY, new byte[0], 16);

    // client_handshake_iv = hkdf-Expand-Label(
    //    key = client_handshake_traffic_secret,
    //    label = "iv",
    //    context = "",
    //    len = 12)
    byte[] clientHandshakeIV = HKDF.expandLabel(clientHandshakeTrafficSecret, IV, new byte[0], 12);

    // server_handshake_iv = hkdf-Expand-Label(
    //    key = server_handshake_traffic_secret,
    //    label = "iv",
    //    context = "",
    //    len = 12)
    byte[] serverHandshakeIV = HKDF.expandLabel(serverHandshakeTrafficSecret, IV, new byte[0], 12);

    byte[] clientPnKey = HKDF.expandLabel(clientHandshakeTrafficSecret, HP_KEY, new byte[0], 16);
    byte[] serverPnKey = HKDF.expandLabel(serverHandshakeTrafficSecret, HP_KEY, new byte[0], 16);

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
}
