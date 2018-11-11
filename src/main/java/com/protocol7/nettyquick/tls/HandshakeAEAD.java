package com.protocol7.nettyquick.tls;

import at.favre.lib.crypto.HKDF;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import static com.protocol7.nettyquick.utils.Hex.dehex;
import static com.protocol7.nettyquick.utils.Hex.hex;

public class HandshakeAEAD {

    private static final HKDF hkdf = HKDF.fromHmacSha256();
    // early_secret = HKDF-Extract(
    //         salt=00,
    //         key=00...)
    private static final byte[] earlySecret = hkdf.extract(dehex("00"), dehex("0000000000000000000000000000000000000000000000000000000000000000"));
    private static final byte[] emptyHash = Hashing.sha256().hashString("", Charsets.US_ASCII).asBytes();
    public static final String TLS_13_LABEL_PREFIX = "tls13 ";


    public static AEAD create(byte[] sharedSecret, byte[] helloHash, boolean quic, boolean isClient) {
        String labelPrefix;
        if (quic) {
            labelPrefix = "quic ";
        } else {
            labelPrefix = TLS_13_LABEL_PREFIX;
        }


        //         derived_secret = HKDF-Expand-Label(
        //                key = early_secret,
        //                label = "derived",
        //                context = empty_hash,
        //                len = 32)
        byte[] derivedSecret = expandLabel(hkdf, earlySecret, TLS_13_LABEL_PREFIX,"derived", emptyHash, 32);

        //         handshake_secret = HKDF-Extract(
        //                salt = derived_secret,
        //                key = shared_secret)
        byte[] handshakeSecret = hkdf.extract(derivedSecret, sharedSecret);

        // client_handshake_traffic_secret = HKDF-Expand-Label(
        //    key = handshake_secret,
        //    label = "c hs traffic",
        //    context = hello_hash,
        //    len = 32)
        byte[] clientHandshakeTrafficSecret = expandLabel(hkdf, handshakeSecret, "tls13 ","c hs traffic", helloHash, 32);

        System.out.println("clientHandshakeTrafficSecret: " + hex(clientHandshakeTrafficSecret));

        //server_handshake_traffic_secret = HKDF-Expand-Label(
        //    key = handshake_secret,
        //    label = "s hs traffic",
        //    context = hello_hash,
        //    len = 32)
        byte[] serverHandshakeTrafficSecret = expandLabel(hkdf, handshakeSecret, "tls13 ","s hs traffic", helloHash, 32);

        System.out.println("serverHandshakeTrafficSecret: " + hex(serverHandshakeTrafficSecret));

        // client_handshake_key = HKDF-Expand-Label(
        //    key = client_handshake_traffic_secret,
        //    label = "key",
        //    context = "",
        //    len = 16)
        byte[] clientHandshakeKey = expandLabel(hkdf, clientHandshakeTrafficSecret, labelPrefix, "key", new byte[0], 16);

        //server_handshake_key = HKDF-Expand-Label(
        //    key = server_handshake_traffic_secret,
        //    label = "key",
        //    context = "",
        //    len = 16)
        byte[] serverHandshakeKey = expandLabel(hkdf, serverHandshakeTrafficSecret, labelPrefix,"key", new byte[0], 16);


        //client_handshake_iv = HKDF-Expand-Label(
        //    key = client_handshake_traffic_secret,
        //    label = "iv",
        //    context = "",
        //    len = 12)
        byte[] clientHandshakeIV = expandLabel(hkdf, clientHandshakeTrafficSecret, labelPrefix,"iv", new byte[0], 12);


        //server_handshake_iv = HKDF-Expand-Label(
        //    key = server_handshake_traffic_secret,
        //    label = "iv",
        //    context = "",
        //    len = 12)
        byte[] serverHandshakeIV = expandLabel(hkdf, serverHandshakeTrafficSecret, labelPrefix,"iv", new byte[0], 12);

        if (isClient) {
            return new AEAD(clientHandshakeKey, serverHandshakeKey, clientHandshakeIV, serverHandshakeIV);
        } else {
            return new AEAD(serverHandshakeKey, clientHandshakeKey, serverHandshakeIV, clientHandshakeIV);
        }
    }

    private static byte[] expandLabel(HKDF hkdf, byte[] key, String labelPrefix, String label, byte[] context, int length) {
        byte[] expandedLabel = makeLabel(labelPrefix, label, context, length);
        return hkdf.expand(key, expandedLabel, length);
    }

    private static byte[] makeLabel(String labelPrefix, String label, byte[] context, int length) {
        byte[] expandedLabel = (labelPrefix + label).getBytes(Charsets.US_ASCII);

        ByteBuf bb = Unpooled.buffer();
        bb.writeShort(length);
        bb.writeByte(expandedLabel.length);

        bb.writeBytes(expandedLabel);

        bb.writeByte(context.length);
        bb.writeBytes(context);

        return Bytes.asArray(bb);
    }
}
