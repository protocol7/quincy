package com.protocol7.quincy.tls;

import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.KeyAgreement;

public class KeyExchange {

  private static final int PKCS_PUBLIC_PREFIX_LENGTH = 14;
  private static final int PKCS_PRIVATE_PREFIX_LENGTH = 16;
  private static final byte[] PKCS_PUBLIC_PREFIX_X25519 = Hex.dehex("302c300706032b656e0500032100");
  private static final byte[] PKCS_PUBLIC_PREFIX_X448 = Hex.dehex("3044300706032b656f0500033900");

  public static KeyExchange generate(final Group group) {
    try {
      final KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(group.name());
      return new KeyExchange(group, keyPairGen.generateKeyPair());
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private final Group group;
  private final KeyPair keyPair;

  private KeyExchange(final Group group, final KeyPair keyPair) {
    this.group = group;
    this.keyPair = keyPair;
  }

  public byte[] getPrivateKey() {
    final byte[] privateKey = keyPair.getPrivate().getEncoded();
    return Arrays.copyOfRange(privateKey, PKCS_PRIVATE_PREFIX_LENGTH, privateKey.length);
  }

  public byte[] getPublicKey() {
    final byte[] publicKey = keyPair.getPublic().getEncoded();
    return Arrays.copyOfRange(publicKey, PKCS_PUBLIC_PREFIX_LENGTH, publicKey.length);
  }

  public Group getGroup() {
    return group;
  }

  public byte[] generateSharedSecret(final byte[] otherPublicKey) {
    try {
      final KeyFactory keyFactory = KeyFactory.getInstance(group.name());
      final X509EncodedKeySpec x509KeySpec =
          new X509EncodedKeySpec(Bytes.concat(pkcsPublicPrefix(group), otherPublicKey));
      final PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);

      final KeyAgreement keyAgree = KeyAgreement.getInstance(group.name());
      keyAgree.init(keyPair.getPrivate());
      keyAgree.doPhase(pubKey, true);
      return keyAgree.generateSecret();
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] pkcsPublicPrefix(final Group group) {
    switch (group) {
      case X25519:
        return PKCS_PUBLIC_PREFIX_X25519;
      case X448:
        return PKCS_PUBLIC_PREFIX_X448;
      default:
        throw new IllegalArgumentException("Unknown group");
    }
  }
}
