package com.protocol7.quincy.protocol.frames;

import com.protocol7.quincy.protocol.PacketNumber;
import org.junit.Test;

public class AckBlockTest {

  @Test
  public void cstrValidation() {
    new AckBlock(new PacketNumber(123), new PacketNumber(124));
    new AckBlock(new PacketNumber(123), new PacketNumber(123));
  }

  @Test(expected = IllegalArgumentException.class)
  public void cstrValidationFail() {
    new AckBlock(new PacketNumber(124), new PacketNumber(123));
  }
}
