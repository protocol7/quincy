package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class DataBlockedFrameTest {

  @Test
  public void roundtrip() {
    final DataBlockedFrame frame = new DataBlockedFrame(456);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final DataBlockedFrame parsed = DataBlockedFrame.parse(bb);

    assertEquals(frame.getDataLimit(), parsed.getDataLimit());
  }
}
