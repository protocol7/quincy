package com.protocol7.nettyquick.tls;

import at.favre.lib.crypto.HKDF;
import com.google.common.base.Charsets;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class HKDFUtil {

  public static final String TLS_13_LABEL_PREFIX = "tls13 ";
  public static final String QUIC_LABEL_PREFIX = "quic ";

  public static final HKDF hkdf = at.favre.lib.crypto.HKDF.fromHmacSha256();

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
  private static final byte[] DERIVED_SECRET =
      expandLabel(EARLY_SECRET, TLS_13_LABEL_PREFIX, "derived", EMPTY_HASH, 32);

  public static byte[] calculateHandshakeSecret(byte[] sharedSecret) {
    //         handshake_secret = hkdf-Extract(
    //                salt = derived_secret,
    //                key = shared_secret)
    return hkdf.extract(DERIVED_SECRET, sharedSecret);
  }

  public static byte[] expandLabel(
      byte[] key, String labelPrefix, String label, byte[] context, int length) {
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

    return Bytes.drainToArray(bb);
  }
}
