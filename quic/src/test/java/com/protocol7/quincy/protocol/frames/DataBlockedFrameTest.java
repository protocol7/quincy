package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class DataBlockedFrameTest {

  @Test
  public void roundtrip() {
    DataBlockedFrame frame = new DataBlockedFrame(456);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    DataBlockedFrame parsed = DataBlockedFrame.parse(bb);

    assertEquals(frame.getDataLimit(), parsed.getDataLimit());
  }
}
