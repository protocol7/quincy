package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class MaxStreamsFrameTest {

  @Test
  public void roundtripBidi() {
    MaxStreamsFrame frame = new MaxStreamsFrame(456, true);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x12, bb.getByte(0));

    MaxStreamsFrame parsed = MaxStreamsFrame.parse(bb);

    assertEquals(frame.getMaxStreams(), parsed.getMaxStreams());
  }

  @Test
  public void roundtripUni() {
    MaxStreamsFrame frame = new MaxStreamsFrame(456, false);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x13, bb.getByte(0));

    MaxStreamsFrame parsed = MaxStreamsFrame.parse(bb);

    assertEquals(frame.getMaxStreams(), parsed.getMaxStreams());
    assertEquals(frame.isBidi(), parsed.isBidi());
  }
}
