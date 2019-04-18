package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class MaxStreamsFrameTest {

  @Test
  public void roundtripBidi() {
    final MaxStreamsFrame frame = new MaxStreamsFrame(456, true);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x12, bb.getByte(0));

    final MaxStreamsFrame parsed = MaxStreamsFrame.parse(bb);

    assertEquals(frame.getMaxStreams(), parsed.getMaxStreams());
  }

  @Test
  public void roundtripUni() {
    final MaxStreamsFrame frame = new MaxStreamsFrame(456, false);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x13, bb.getByte(0));

    final MaxStreamsFrame parsed = MaxStreamsFrame.parse(bb);

    assertEquals(frame.getMaxStreams(), parsed.getMaxStreams());
    assertEquals(frame.isBidi(), parsed.isBidi());
  }
}
