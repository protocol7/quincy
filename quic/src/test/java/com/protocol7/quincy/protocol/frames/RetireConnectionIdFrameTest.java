package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class RetireConnectionIdFrameTest {

  @Test
  public void roundtrip() {
    final RetireConnectionIdFrame rcif = new RetireConnectionIdFrame(123);

    final ByteBuf bb = Unpooled.buffer();
    rcif.write(bb);

    final RetireConnectionIdFrame parsed = RetireConnectionIdFrame.parse(bb);

    assertEquals(rcif.getSequenceNumber(), parsed.getSequenceNumber());
  }
}
