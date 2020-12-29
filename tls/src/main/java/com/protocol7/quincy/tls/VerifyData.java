package com.protocol7.quincy.tls;

import static com.google.common.base.Preconditions.checkArgument;
import static com.protocol7.quincy.tls.aead.Labels.FINISHED;

import com.google.common.hash.Hashing;

public class VerifyData {

  public static byte[] create(final byte[] handshakeTrafficSecret, final byte[] finishedHash) {
    checkArgument(handshakeTrafficSecret.length == 32);
    checkArgument(finishedHash.length == 32);

    // finished_key = HKDF-Expand-Label(
    //    key = client_handshake_traffic_secret,
    //    label = "finished",
    //    context = "",
    //    len = 32)
    final byte[] finishedKey = HKDF.expandLabel(handshakeTrafficSecret, FINISHED, new byte[0], 32);

    // verify_data = HMAC-SHA256(
    //	key = finished_key,
    //	msg = finished_hash)
    return Hashing.hmacSha256(finishedKey).hashBytes(finishedHash).asBytes();
  }

  public static boolean verify(
      final byte[] verifyData,
      final byte[] handshakeTrafficSecret,
      final byte[] finishedHash,
      final boolean quic) {
    checkArgument(verifyData.length > 0);
    checkArgument(handshakeTrafficSecret.length == 32);
    checkArgument(finishedHash.length == 32);

    final byte[] actual = create(handshakeTrafficSecret, finishedHash);

    return ConstantTimeEquals.isEqual(verifyData, actual);
  }
}
