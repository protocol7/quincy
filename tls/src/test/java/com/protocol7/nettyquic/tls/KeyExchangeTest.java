package com.protocol7.nettyquic.tls;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Hex;
import org.junit.Test;

import javax.crypto.KeyAgreement;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.NamedParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class KeyExchangeTest {

  @Test
  public void verifyKeys() {
    KeyExchange keys = KeyExchange.generate(Group.X25519);

    assertEquals(32, keys.getPrivateKey().length);
    assertEquals(32, keys.getPublicKey().length);
  }

  @Test
  public void keyAgreementX25519() {
    KeyExchange alice = KeyExchange.generate(Group.X25519);
    KeyExchange bob = KeyExchange.generate(Group.X25519);

    byte[] aliceShared = alice.generateSharedSecret(bob.getPublicKey());
    byte[] bobShared = bob.generateSharedSecret(alice.getPublicKey());

    assertArrayEquals(aliceShared, bobShared);
  }

  @Test
  public void keyAgreementX448() {
    KeyExchange alice = KeyExchange.generate(Group.X448);
    KeyExchange bob = KeyExchange.generate(Group.X448);

    byte[] aliceShared = alice.generateSharedSecret(bob.getPublicKey());
    byte[] bobShared = bob.generateSharedSecret(alice.getPublicKey());

    assertArrayEquals(aliceShared, bobShared);
  }

  @Test
  public void keyAgreementSecp256r1() {
    KeyExchange alice = KeyExchange.generate(Group.SECP256r1);
    KeyExchange bob = KeyExchange.generate(Group.SECP256r1);

    byte[] aliceShared = alice.generateSharedSecret(bob.getPublicKey());
    byte[] bobShared = bob.generateSharedSecret(alice.getPublicKey());

    assertArrayEquals(aliceShared, bobShared);
  }

  @Test
  public void dummy() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, InvalidKeySpecException {
    Group group = Group.X25519;

    KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(group.getKeyPairGeneratorAlgo());
    keyPairGen.initialize(group.getParameterSpec());
    KeyPair keyPair1 = keyPairGen.generateKeyPair();

    System.out.println(Hex.hex(keyPair1.getPublic().getEncoded()));
    System.out.println(keyPair1.getPublic().getClass());
    System.out.println(keyPair1.getPublic().getFormat());
    System.out.println(keyPair1.getPublic().getAlgorithm());
  }

  @Test
  public void name() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
    System.out.println("3041020100301306072a8648ce3d020106082a8648ce3d030107042730250201010420");
    System.out.println("3059301306072a8648ce3d020106082a8648ce3d03010703420004");
    System.out.println("3044300706032b656f0500033900");
    for (int i = 0; i < 25; i++) {
      KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("EC");
      keyPairGen.initialize(new ECGenParameterSpec("secp256r1"));
      KeyPair keyPair1 = keyPairGen.generateKeyPair();
      System.out.println(Hex.hex(keyPair1.getPrivate().getEncoded()));
    }
  }
}
