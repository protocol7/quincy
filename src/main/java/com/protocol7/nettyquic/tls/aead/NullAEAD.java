package com.protocol7.nettyquic.tls.aead;

import at.favre.lib.crypto.HKDF;
import com.google.common.base.Charsets;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NullAEAD {

  private static final byte[] QUIC_VERSION_1_SALT =
      Hex.dehex("9c108f98520a5c5c32968e950e8a2c5fe06d6c38");

  public static AEAD create(ConnectionId connId, boolean isClient) {
    HKDF hkdf = HKDF.fromHmacSha256();
    byte[] initialSecret = hkdf.extract(QUIC_VERSION_1_SALT, connId.asBytes());

    int length = 32;

    byte[] clientSecret = expand(hkdf, initialSecret, "quic client in", length);
    byte[] serverSecret = expand(hkdf, initialSecret, "quic server in", length);

    byte[] mySecret;
    byte[] otherSecret;
    if (isClient) {
      mySecret = clientSecret;
      otherSecret = serverSecret;
    } else {
      mySecret = serverSecret;
      otherSecret = clientSecret;
    }

    byte[] myKey = expand(hkdf, mySecret, "quic key", 16);
    byte[] myIV = expand(hkdf, mySecret, "quic iv", 12);

    byte[] otherKey = expand(hkdf, otherSecret, "quic key", 16);
    byte[] otherIV = expand(hkdf, otherSecret, "quic iv", 12);

    byte[] myPnKey = expand(hkdf, mySecret, "quic hp", 16);
    byte[] otherPnKey = expand(hkdf, otherSecret, "quic hp", 16);

    return new AEAD(myKey, otherKey, myIV, otherIV, myPnKey, otherPnKey);
  }

  private static byte[] expand(HKDF hkdf, byte[] secret, String label, int length) {
    return hkdf.expand(secret, makeLabel(label, length), length);
  }

  private static byte[] makeLabel(String label, int length) {
    ByteBuf bb = Unpooled.buffer();
    bb.writeShort(length);
    bb.writeByte(label.length());
    bb.writeBytes(label.getBytes(Charsets.US_ASCII));
    bb.writeByte(0);

    return Bytes.drainToArray(bb);
  }
}
