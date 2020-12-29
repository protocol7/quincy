package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamsBlockedFrameTest {

  @Test
  public void roundtripBidi() {
    final StreamsBlockedFrame frame = new StreamsBlockedFrame(456, true);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x16, bb.getByte(0));

    final StreamsBlockedFrame parsed = StreamsBlockedFrame.parse(bb);

    assertEquals(frame.getMaxStreams(), parsed.getMaxStreams());
    assertEquals(frame.isBidi(), parsed.isBidi());
  }

  @Test
  public void roundtripUni() {
    final StreamsBlockedFrame frame = new StreamsBlockedFrame(456, false);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x17, bb.getByte(0));

    final StreamsBlockedFrame parsed = StreamsBlockedFrame.parse(bb);

    assertEquals(frame.getMaxStreams(), parsed.getMaxStreams());
    assertEquals(frame.isBidi(), parsed.isBidi());
  }
}
