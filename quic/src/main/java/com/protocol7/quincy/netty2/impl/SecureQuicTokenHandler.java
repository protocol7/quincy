package com.protocol7.quincy.netty2.impl;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.netty2.api.QuicTokenHandler;
import com.protocol7.quincy.tls.ConstantTimeEquals;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Ticker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.NetUtil;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class SecureQuicTokenHandler implements QuicTokenHandler {

  private static final String HMAC_SHA512 = "HmacSHA512";
  private static final int HMAC_LENGTH = 64;

  private final PrivateKey key;
  private final long ttlMs;
  private final Ticker ticker;

  public SecureQuicTokenHandler(
      final PrivateKey key, final long ttl, final TimeUnit timeUnit, final Ticker ticker) {
    this.key = requireNonNull(key);
    this.ttlMs = timeUnit.toMillis(ttl);
    this.ticker = ticker;
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

  @Override
  public boolean writeToken(
      final ByteBuf out, final ByteBuf dcid, final InetSocketAddress address) {
    requireNonNull(address);

    final ByteBuf bb = Unpooled.buffer();
    try {
      final byte[] addressBytes = address.getAddress().getAddress();
      bb.writeByte(addressBytes.length);
      bb.writeBytes(addressBytes);
      Varint.write(ticker.milliTime() + ttlMs, bb); // write deadline

      final byte[] data = Bytes.peekToArray(bb);

      bb.writeBytes(hmac(data));

      out.writeBytes(bb);
      return true;
    } finally {
      bb.release();
    }
  }

  @Override
  public int validateToken(final ByteBuf token, final InetSocketAddress address) {
    requireNonNull(token);
    requireNonNull(address);

    final int bbOffset = token.readerIndex();

    try {
      final int addressLen = token.readByte();
      final byte[] addressBytes = new byte[addressLen];
      token.readBytes(addressBytes);

      if (!ConstantTimeEquals.isEqual(addressBytes, address.getAddress().getAddress())) {
        return -1;
      }

      final long deadline = Varint.readAsLong(token);
      if (deadline < ticker.milliTime()) {
        return -1;
      }

      final byte[] data = new byte[token.readerIndex()];
      token.getBytes(bbOffset, data);

      final byte[] actualHmac = new byte[HMAC_LENGTH];
      token.readBytes(actualHmac);
      final byte[] expectedHmac = hmac(data);

      if (!ConstantTimeEquals.isEqual(expectedHmac, actualHmac)) {
        return -1;
      }

      return token.readerIndex() - bbOffset;
    } catch (final IndexOutOfBoundsException | NegativeArraySizeException e) {
      // invalid token
      return -1;
    }
  }

  @Override
  public int maxTokenLength() {
    // address length + address + varint max length
    return 1 + NetUtil.LOCALHOST6.getAddress().length + Varint.MAX_LENGTH + HMAC_LENGTH;
  }
}
