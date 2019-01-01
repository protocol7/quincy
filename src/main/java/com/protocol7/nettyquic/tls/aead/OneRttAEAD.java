package com.protocol7.nettyquic.tls.aead;

import com.protocol7.nettyquic.tls.HKDFUtil;

public class OneRttAEAD {

  private static final byte[] ZEROS = new byte[32];
  private static final byte[] EMPTY = new byte[0];

  public static AEAD create(
      byte[] handshakeSecret, byte[] handshakeHash, boolean quic, boolean isClient) {
    String labelPrefix = getPrefix(quic);

    // derived_secret = HKDF-Expand-Label(
    //                key = handshake_secret,
    //                label = "derived",
    //                context = empty_hash,
    //                len = 32)
    byte[] derivedSecret =
        HKDFUtil.expandLabel(handshakeSecret, "tls13 ", "derived", HKDFUtil.EMPTY_HASH, 32);

    //        master_secret = HKDF-Extract(
    //                salt=derived_secret,
    //                key=00...)
    byte[] masterSecret = HKDFUtil.hkdf.extract(derivedSecret, ZEROS);

    // client_application_traffic_secret = HKDF-Expand-Label(
    //    key = master_secret,
    //    label = "c ap traffic",
    //    context = handshake_hash,
    //    len = 32)
    byte[] clientApplicationTrafficSecret =
        HKDFUtil.expandLabel(masterSecret, "tls13 ", "c ap traffic", handshakeHash, 32);

    // server_application_traffic_secret = HKDF-Expand-Label(
    //    key = master_secret,
    //    label = "s ap traffic",
    //    context = handshake_hash,
    //    len = 32)
    byte[] serverApplicationTrafficSecret =
        HKDFUtil.expandLabel(masterSecret, "tls13 ", "s ap traffic", handshakeHash, 32);

    // client_application_key = HKDF-Expand-Label(
    //    key = client_application_traffic_secret,
    //    label = "key",
    //    context = "",
    //    len = 16)
    byte[] clientApplicationKey =
        HKDFUtil.expandLabel(clientApplicationTrafficSecret, labelPrefix, "key", EMPTY, 16);

    // server_application_key = HKDF-Expand-Label(
    //    key = server_application_traffic_secret,
    //    label = "key",
    //    context = "",
    //    len = 16)
    byte[] serverApplicationKey =
        HKDFUtil.expandLabel(serverApplicationTrafficSecret, labelPrefix, "key", EMPTY, 16);

    // client_application_iv = HKDF-Expand-Label(
    //    key = client_application_traffic_secret,
    //    label = "iv",
    //    context = "",
    //    len = 12)
    byte[] clientApplicationIV =
        HKDFUtil.expandLabel(clientApplicationTrafficSecret, labelPrefix, "iv", EMPTY, 12);

    // server_application_iv = HKDF-Expand-Label(
    //    key = server_application_traffic_secret,
    //    label = "iv",
    //    context = "",
    //    len = 12)
    byte[] serverApplicationIV =
        HKDFUtil.expandLabel(serverApplicationTrafficSecret, labelPrefix, "iv", EMPTY, 12);

    if (isClient) {
      return new AEAD(
          clientApplicationKey, serverApplicationKey, clientApplicationIV, serverApplicationIV);
    } else {
      return new AEAD(
          serverApplicationKey, clientApplicationKey, serverApplicationIV, clientApplicationIV);
    }
  }

  private static String getPrefix(boolean quic) {
    String labelPrefix;
    if (quic) {
      labelPrefix = HKDFUtil.QUIC_LABEL_PREFIX;
    } else {
      labelPrefix = HKDFUtil.TLS_13_LABEL_PREFIX;
    }
    return labelPrefix;
  }
}
