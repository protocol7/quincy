package com.protocol7.quincy.protocol.frames;

import org.junit.Test;

public class AckBlockTest {

  @Test
  public void cstrValidation() {
    new AckBlock(123, 124);
    new AckBlock(123, 123);
  }

  @Test(expected = IllegalArgumentException.class)
  public void cstrValidationFail() {
    new AckBlock(124, 123);
  }
}
