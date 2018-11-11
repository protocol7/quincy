package com.protocol7.nettyquick.tls;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class KeyExchangeKeysTest {

    @Test
    public void verifyKeys() {
        KeyExchangeKeys keys = KeyExchangeKeys.generate(Group.X25519);

        assertEquals(32, keys.getPrivateKey().length);
        assertEquals(32, keys.getPublicKey().length);
    }

    @Test
    public void keyAgreement() {
        KeyExchangeKeys alice = KeyExchangeKeys.generate(Group.X25519);
        KeyExchangeKeys bob = KeyExchangeKeys.generate(Group.X25519);

        byte[] aliceShared = alice.generateSharedSecret(bob.getPublicKey());
        byte[] bobShared = bob.generateSharedSecret(alice.getPublicKey());

        assertArrayEquals(aliceShared, bobShared);
    }
}