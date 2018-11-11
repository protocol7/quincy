package com.protocol7.nettyquick.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BitsTest {

  @Test
  public void testSet() {
    assertEquals(3, Bits.set(2, 0));
    assertEquals(3, Bits.set(1, 1));
    assertEquals(1, Bits.set(1, 0));
  }

  @Test
  public void testUnset() {
    assertEquals(2, Bits.unset(3, 0));
    assertEquals(0, Bits.unset(1, 0));
    assertEquals(1, Bits.unset(1, 1));
  }
}