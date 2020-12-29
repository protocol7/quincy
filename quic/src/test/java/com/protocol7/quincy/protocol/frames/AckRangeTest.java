package com.protocol7.quincy.protocol.frames;

import org.junit.Test;

public class AckRangeTest {

  @Test
  public void cstrValidation() {
    new AckRange(123, 124);
    new AckRange(123, 123);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cstrValidationFail() {
    new AckRange(124, 123);
  }
}
