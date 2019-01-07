package com.protocol7.nettyquic.tls;

import com.google.common.base.Charsets;
import com.protocol7.nettyquic.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class HKDF {

  private static final String TLS_13_LABEL_PREFIX = "tls13 ";

  public static final at.favre.lib.crypto.HKDF hkdf = at.favre.lib.crypto.HKDF.fromHmacSha256();

  // early_secret = hkdf-Extract(
  //         salt=00,
  //         key=00...)
  private static final byte[] EARLY_SECRET = hkdf.extract(new byte[1], new byte[32]);
  public static final byte[] EMPTY_HASH = Hash.sha256("".getBytes(Charsets.US_ASCII));

  //         derived_secret = hkdf-Expand-Label(
  //                key = early_secret,
  //                label = "derived",
  //                context = empty_hash,
  //                len = 32)
  private static final byte[] DERIVED_SECRET = expandLabel(EARLY_SECRET, "derived", EMPTY_HASH, 32);

  public static byte[] calculateHandshakeSecret(byte[] sharedSecret) {
    //         handshake_secret = hkdf-Extract(
    //                salt = derived_secret,
    //                key = shared_secret)
    return hkdf.extract(DERIVED_SECRET, sharedSecret);
  }

  public static byte[] extract(byte[] salt, byte[] inputKeyingMaterial) {
    return hkdf.extract(salt, inputKeyingMaterial);
  }

  public static byte[] expandLabel(byte[] key, String label, int length) {
    byte[] expandedLabel = (TLS_13_LABEL_PREFIX + label).getBytes(Charsets.US_ASCII);
    return hkdf.expand(key, expandedLabel, length);
  }

  public static byte[] expandLabel(byte[] key, String label, byte[] context, int length) {
    byte[] expandedLabel = makeLabel(label, context, length);
    return hkdf.expand(key, expandedLabel, length);
  }

  private static byte[] makeLabel(String label, byte[] context, int length) {
    byte[] expandedLabel = (TLS_13_LABEL_PREFIX + label).getBytes(Charsets.US_ASCII);

    ByteBuf bb = Unpooled.buffer();
    bb.writeShort(length);
    bb.writeByte(expandedLabel.length);

    bb.writeBytes(expandedLabel);

    bb.writeByte(context.length);
    bb.writeBytes(context);

    return Bytes.drainToArray(bb);
  }
}
