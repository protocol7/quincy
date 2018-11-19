package com.protocol7.nettyquick.tls;

import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;

public class VerifyData {

    public static byte[] create(byte[] handshakeTrafficSecret, byte[] finishedHash, boolean quic) {
        Preconditions.checkArgument(handshakeTrafficSecret.length == 32);
        Preconditions.checkArgument(finishedHash.length == 32);


        String labelPrefix;
        if (quic) {
            labelPrefix = HKDFUtil.QUIC_LABEL_PREFIX;
        } else {
            labelPrefix = HKDFUtil.TLS_13_LABEL_PREFIX;
        }

        // finished_key = HKDF-Expand-Label(
        //    key = client_handshake_traffic_secret,
        //    label = "finished",
        //    context = "",
        //    len = 32)
        byte[] finishedKey = HKDFUtil.expandLabel(handshakeTrafficSecret, labelPrefix, "finished", new byte[0], 32);

        //verify_data = HMAC-SHA256(
        //	key = finished_key,
        //	msg = finished_hash)
        return Hashing.hmacSha256(finishedKey).hashBytes(finishedHash).asBytes();
    }

    public static boolean verify(byte[] verifyData, byte[] handshakeTrafficSecret, byte[] finishedHash, boolean quic) {
        byte[] actual = create(handshakeTrafficSecret, finishedHash, quic);

        return CryptoEquals.isEqual(verifyData, actual);
    }



}
