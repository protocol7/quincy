package com.protocol7.nettyquick.tls.messages;

import com.google.common.hash.Hashing;
import com.protocol7.nettyquick.tls.CryptoEquals;
import com.protocol7.nettyquick.tls.HKDFUtil;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;

public class ClientFinished {

    public static ClientFinished create(byte[] clientHandshakeTrafficSecret, byte[] finHash, boolean quic) {
        byte[] verifyData = createVerifyData(clientHandshakeTrafficSecret, finHash, quic);

        return new ClientFinished(verifyData);
    }

    private static byte[] createVerifyData(byte[] clientHandshakeTrafficSecret, byte[] finHash, boolean quic) {
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
        byte[] finishedKey = HKDFUtil.expandLabel(clientHandshakeTrafficSecret, labelPrefix, "finished", new byte[0], 32);

        //finished_hash = SHA256(Client Hello ... Server Finished)

        //verify_data = HMAC-SHA256(
        //	key = finished_key,
        //	msg = finished_hash)
        return Hashing.hmacSha256(finishedKey).hashBytes(finHash).asBytes();
    }

    public static boolean verify(byte[] verifyData, byte[] clientHandshakeTrafficSecret, byte[] finHash, boolean quic) {
        byte[] actual = createVerifyData(clientHandshakeTrafficSecret, finHash, quic);

        return CryptoEquals.isEqual(verifyData, actual);
    }


    public static ClientFinished parse(ByteBuf bb) {
        int type = bb.readByte();
        if (type != 0x14) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }

        int len = Bytes.read24(bb);
        byte[] verifyData = new byte[len];
        bb.readBytes(verifyData);

        return new ClientFinished(verifyData);
    }

    private final byte[] verificationData;

    public ClientFinished(byte[] verificationData) {
        this.verificationData = verificationData;
    }

    public byte[] getVerificationData() {
        return verificationData;
    }

    public void write(ByteBuf bb) {
        bb.writeByte(0x14);

        Bytes.write24(bb, verificationData.length);
        bb.writeBytes(verificationData);
    }
}
