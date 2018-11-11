package com.protocol7.nettyquick.tls;

import org.junit.Test;

import static com.protocol7.nettyquick.tls.Group.SECP256R1;
import static com.protocol7.nettyquick.tls.Group.SECP384R1;
import static com.protocol7.nettyquick.tls.Group.X25519;
import static org.junit.Assert.*;

public class GroupTest {

    @Test
    public void fromValue() {
        assertEquals(X25519, Group.fromValue(X25519.getValue()));
        assertEquals(SECP256R1, Group.fromValue(SECP256R1.getValue()));
        assertEquals(SECP384R1, Group.fromValue(SECP384R1.getValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownFromValue() {
        Group.fromValue(123);
    }
}