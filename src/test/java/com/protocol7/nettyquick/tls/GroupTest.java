package com.protocol7.nettyquick.tls;

import org.junit.Test;

import static com.protocol7.nettyquick.tls.Group.*;
import static com.protocol7.nettyquick.tls.Group.X25519;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GroupTest {

    @Test
    public void fromValue() {
        assertEquals(X25519, Group.fromValue(X25519.getValue()).get());
        assertEquals(X448, Group.fromValue(X448.getValue()).get());
    }

    @Test
    public void unknownFromValue() {
        assertFalse(Group.fromValue(123).isPresent());
    }
}