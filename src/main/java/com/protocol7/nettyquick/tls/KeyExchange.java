package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.KeyAgreement;

public class KeyExchange {

  private static final int PKCS_PUBLIC_PREFIX_LENGTH = 14;
  private static final int PKCS_PRIVATE_PREFIX_LENGTH = 16;
  private static final byte[] PKCS_PUBLIC_PREFIX_X25519 = Hex.dehex("302c300706032b656e0500032100");
  private static final byte[] PKCS_PUBLIC_PREFIX_X448 = Hex.dehex("3044300706032b656f0500033900");

  public static KeyExchange generate(Group group) {
    try {
      KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(group.name());
      return new KeyExchange(group, keyPairGen.generateKeyPair());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private final Group group;
  private final KeyPair keyPair;

  private KeyExchange(Group group, KeyPair keyPair) {
    this.group = group;
    this.keyPair = keyPair;
  }

  public byte[] getPrivateKey() {
    byte[] privateKey = keyPair.getPrivate().getEncoded();
    return Arrays.copyOfRange(privateKey, PKCS_PRIVATE_PREFIX_LENGTH, privateKey.length);
  }

  public byte[] getPublicKey() {
    byte[] publicKey = keyPair.getPublic().getEncoded();
    return Arrays.copyOfRange(publicKey, PKCS_PUBLIC_PREFIX_LENGTH, publicKey.length);
  }

  public Group getGroup() {
    return group;
  }

  public byte[] generateSharedSecret(byte[] otherPublicKey) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance(group.name());
      X509EncodedKeySpec x509KeySpec =
          new X509EncodedKeySpec(Bytes.concat(pkcsPublicPrefix(group), otherPublicKey));
      PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);

      KeyAgreement keyAgree = KeyAgreement.getInstance(group.name());
      keyAgree.init(keyPair.getPrivate());
      keyAgree.doPhase(pubKey, true);
      return keyAgree.generateSecret();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] pkcsPublicPrefix(Group group) {
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
