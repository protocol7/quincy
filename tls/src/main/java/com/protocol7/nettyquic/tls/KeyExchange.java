package com.protocol7.nettyquic.tls;

import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Hex;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import javax.crypto.KeyAgreement;

// https://github.com/openjdk/jdk/blob/b0f3d731b0c8a32c8776695031383c4dea01786b/test/jdk/java/security/KeyAgreement/KeyAgreementTest.java
public class KeyExchange {

  public static KeyExchange generate(Group group) {
    try {
      KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(group.getKeyPairGeneratorAlgo());
      keyPairGen.initialize(group.getParameterSpec());
      return new KeyExchange(group, keyPairGen.generateKeyPair());
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
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
    return Arrays.copyOfRange(privateKey, group.getPkcsPrivatePrefix().length, privateKey.length);
  }

  public byte[] getPublicKey() {
    System.out.println(keyPair.getPublic().getFormat());
    System.out.println(keyPair.getPrivate().getFormat());
    byte[] publicKey = keyPair.getPublic().getEncoded();
    return Arrays.copyOfRange(publicKey, group.getPkcsPublicPrefix().length, publicKey.length);
  }

  public Group getGroup() {
    return group;
  }

  public byte[] generateSharedSecret(byte[] otherPublicKey) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance(group.getKeyPairGeneratorAlgo());
      X509EncodedKeySpec x509KeySpec =
          new X509EncodedKeySpec(Bytes.concat(group.getPkcsPublicPrefix(), otherPublicKey));
      PublicKey pubKey = keyFactory.generatePublic(x509KeySpec);

      KeyAgreement keyAgree = KeyAgreement.getInstance(group.getKeyAgreementAlgo());
      keyAgree.init(keyPair.getPrivate());
      keyAgree.doPhase(pubKey, true);
      return keyAgree.generateSecret();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
