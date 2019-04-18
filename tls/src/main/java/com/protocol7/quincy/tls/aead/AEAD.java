package com.protocol7.quincy.tls.aead;

import com.google.common.primitives.Longs;
import com.protocol7.quincy.utils.Hex;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AEAD {

  public static final int OVERHEAD = 16;

  private static byte[] makeNonce(final byte[] iv, final long packetNumber) {
    final byte[] nonce = new byte[iv.length];

    System.arraycopy(Longs.toByteArray(packetNumber), 0, nonce, nonce.length - 8, 8);
    for (int i = 0; i < iv.length; i++) {
      nonce[i] ^= iv[i];
    }
    return nonce;
  }

  private static byte[] prepareKey(final byte[] key) {
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    } else if (key.length != 16) {
      throw new IllegalArgumentException("key must be 16 bytes");
    }
    return Arrays.copyOf(key, key.length);
  }

  private static byte[] prepareIV(final byte[] iv) {
    if (iv == null) {
      throw new IllegalArgumentException("IV must not be null");
    } else if (iv.length != 12) {
      throw new IllegalArgumentException("IV must be 12 bytes");
    }
    return Arrays.copyOf(iv, iv.length);
  }

  private final byte[] myKey;
  private final byte[] otherKey;
  private final byte[] myIV;
  private final byte[] otherIV;
  private final byte[] myPnKey;
  private final byte[] otherPnKey;

  public AEAD(
      final byte[] myKey,
      final byte[] otherKey,
      final byte[] myIV,
      final byte[] otherIV,
      final byte[] myPnKey,
      final byte[] otherPnKey) {
    this.myKey = prepareKey(myKey);
    this.otherKey = prepareKey(otherKey);
    this.myIV = prepareIV(myIV);
    this.otherIV = prepareIV(otherIV);
    this.myPnKey = prepareKey(myPnKey);
    this.otherPnKey = prepareKey(otherPnKey);
  }

  public byte[] open(final byte[] src, final long packetNumber, final byte[] aad)
      throws GeneralSecurityException {
    return process(src, packetNumber, aad, otherKey, otherIV, Cipher.DECRYPT_MODE);
  }

  public byte[] seal(final byte[] src, final long packetNumber, final byte[] aad)
      throws GeneralSecurityException {
    return process(src, packetNumber, aad, myKey, myIV, Cipher.ENCRYPT_MODE);
  }

  public int getSampleLength() {
    return 16;
  }

  public byte[] decryptHeader(final byte[] sample, final byte[] bs, final boolean shortHeader)
      throws GeneralSecurityException {
    return processHeader(sample, bs, shortHeader, otherPnKey);
  }

  public byte[] encryptHeader(final byte[] sample, final byte[] bs, final boolean shortHeader)
      throws GeneralSecurityException {
    return processHeader(sample, bs, shortHeader, myPnKey);
  }

  private final ThreadLocal<Cipher> pnCiphers =
      ThreadLocal.withInitial(
          () -> {
            try {
              return Cipher.getInstance("AES/ECB/NoPadding", "SunJCE");
            } catch (final GeneralSecurityException shouldNeverHappen) {
              throw new RuntimeException(shouldNeverHappen);
            }
          });

  private byte[] processHeader(
      final byte[] sample, final byte[] bs, final boolean shortHeader, final byte[] key)
      throws GeneralSecurityException {
    final byte[] out = Arrays.copyOf(bs, bs.length);

    final Cipher cipher = pnCiphers.get();
    final SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");

    cipher.init(Cipher.ENCRYPT_MODE, secretKey);

    final byte[] mask = cipher.doFinal(sample);

    final byte maskMask;
    if (shortHeader) {
      maskMask = 0x1f;
    } else {
      maskMask = 0xf;
    }
    out[0] ^= mask[0] & maskMask;

    for (int i = 1; i < out.length; i++) {
      out[i] ^= mask[i];
    }

    return out;
  }

  private final ThreadLocal<Cipher> aeadCiphers =
      ThreadLocal.withInitial(
          () -> {
            try {
              return Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
            } catch (final GeneralSecurityException shouldNeverHappen) {
              throw new RuntimeException(shouldNeverHappen);
            }
          });

  private byte[] process(
      final byte[] src,
      final long packetNumber,
      final byte[] aad,
      final byte[] key,
      final byte[] iv,
      final int mode)
      throws GeneralSecurityException {
    final Cipher cipher = aeadCiphers.get();
    final SecretKey secretKey = new SecretKeySpec(key, 0, key.length, "AES");
    final byte[] nonce = makeNonce(iv, packetNumber);
    final GCMParameterSpec spec = new GCMParameterSpec(128, nonce);

    cipher.init(mode, secretKey, spec);
    cipher.updateAAD(aad);
    return cipher.doFinal(src);
  }

  public byte[] getMyKey() {
    return myKey;
  }

  public byte[] getOtherKey() {
    return otherKey;
  }

  public byte[] getMyIV() {
    return myIV;
  }

  public byte[] getOtherIV() {
    return otherIV;
  }

  public byte[] getMyPnKey() {
    return myPnKey;
  }

  public byte[] getOtherPnKey() {
    return otherPnKey;
  }

  @Override
  public String toString() {
    return "AEAD{"
        + "myKey="
        + Hex.hex(myKey)
        + ", otherKey="
        + Hex.hex(otherKey)
        + ", myIV="
        + Hex.hex(myIV)
        + ", otherIV="
        + Hex.hex(otherIV)
        + ", myPnKey="
        + Hex.hex(myPnKey)
        + ", otherPnKey="
        + Hex.hex(otherPnKey)
        + '}';
  }
}
