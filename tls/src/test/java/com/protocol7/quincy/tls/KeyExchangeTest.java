package com.protocol7.quincy.tls;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class KeyExchangeTest {

  @Test
  public void verifyKeys() {
    final KeyExchange keys = KeyExchange.generate(Group.X25519);

    assertEquals(32, keys.getPrivateKey().length);
    assertEquals(32, keys.getPublicKey().length);
  }

  @Test
  public void keyAgreementX25519() {
    final KeyExchange alice = KeyExchange.generate(Group.X25519);
    final KeyExchange bob = KeyExchange.generate(Group.X25519);

    final byte[] aliceShared = alice.generateSharedSecret(bob.getPublicKey());
    final byte[] bobShared = bob.generateSharedSecret(alice.getPublicKey());

    assertArrayEquals(aliceShared, bobShared);
  }

  @Test
  public void keyAgreementX448() {
    final KeyExchange alice = KeyExchange.generate(Group.X448);
    final KeyExchange bob = KeyExchange.generate(Group.X448);

    final byte[] aliceShared = alice.generateSharedSecret(bob.getPublicKey());
    final byte[] bobShared = bob.generateSharedSecret(alice.getPublicKey());

    assertArrayEquals(aliceShared, bobShared);
  }
}
