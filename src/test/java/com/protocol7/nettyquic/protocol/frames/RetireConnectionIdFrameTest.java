package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class RetireConnectionIdFrameTest {

  @Test
  public void roundtrip() {
    RetireConnectionIdFrame rcif = new RetireConnectionIdFrame(123);

    ByteBuf bb = Unpooled.buffer();
    rcif.write(bb);

    RetireConnectionIdFrame parsed = RetireConnectionIdFrame.parse(bb);

    assertEquals(rcif.getSequenceNumber(), parsed.getSequenceNumber());
  }
}
