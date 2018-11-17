package com.protocol7.nettyquick.tls;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
}