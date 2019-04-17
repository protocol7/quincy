package com.protocol7.quincy.tls;

import static com.protocol7.quincy.tls.Group.X25519;
import static com.protocol7.quincy.tls.Group.X448;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

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
