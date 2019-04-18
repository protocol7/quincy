package com.protocol7.quincy.addressvalidation;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.tls.CryptoEquals;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class RetryToken {

  private static final String HMAC_SHA512 = "HmacSHA512";

  private final PrivateKey key;

  public RetryToken(final PrivateKey key) {
    this.key = requireNonNull(key);
  }

  public byte[] create(final InetAddress address, final long ttl) {
    requireNonNull(address);

    final byte[] addressBytes = address.getAddress();
    final ByteBuf bb = Unpooled.buffer();
    bb.writeByte(addressBytes.length);
    bb.writeBytes(addressBytes);
    Varint.write(ttl, bb);

    final byte[] data = Bytes.peekToArray(bb);

    bb.writeBytes(hmac(data));

    return Bytes.drainToArray(bb);
  }

  private byte[] hmac(final byte[] data) {
    try {
      final Mac hmac = Mac.getInstance(HMAC_SHA512);
      final SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), HMAC_SHA512);
      hmac.init(keySpec);
      return hmac.doFinal(data);
    } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException("Retry token HMAC generation failed", e);
    }
  }

  public boolean validate(final byte[] token, final InetAddress address, final long maxTtl) {
    requireNonNull(token);
    requireNonNull(address);

    boolean success = true;

    final ByteBuf bb = Unpooled.wrappedBuffer(token);
    try {
      final int addressLen = bb.readByte();
      final byte[] addressBytes = new byte[addressLen];
      bb.readBytes(addressBytes);

      if (!CryptoEquals.isEqual(addressBytes, address.getAddress())) {
        success = false;
      }

      final long ttl = Varint.readAsLong(bb);
      if (ttl < maxTtl) {
        success = false;
      }

      final byte[] data = new byte[bb.readerIndex()];
      bb.getBytes(0, data);

      final byte[] actualHmac = Bytes.drainToArray(bb);
      final byte[] expectedHmac = hmac(data);

      if (!CryptoEquals.isEqual(expectedHmac, actualHmac)) {
        success = false;
      }

      return success;
    } catch (final IndexOutOfBoundsException | NegativeArraySizeException e) {
      // invalid token
      return false;
    }
  }
}
