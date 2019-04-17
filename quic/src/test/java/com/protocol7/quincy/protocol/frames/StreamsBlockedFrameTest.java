package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamsBlockedFrameTest {

  @Test
  public void roundtripBidi() {
    StreamsBlockedFrame frame = new StreamsBlockedFrame(456, true);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x16, bb.getByte(0));

    StreamsBlockedFrame parsed = StreamsBlockedFrame.parse(bb);

    assertEquals(frame.getStreamsLimit(), parsed.getStreamsLimit());
    assertEquals(frame.isBidi(), parsed.isBidi());
  }

  @Test
  public void roundtripUni() {
    StreamsBlockedFrame frame = new StreamsBlockedFrame(456, false);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals(0x17, bb.getByte(0));

    StreamsBlockedFrame parsed = StreamsBlockedFrame.parse(bb);

    assertEquals(frame.getStreamsLimit(), parsed.getStreamsLimit());
    assertEquals(frame.isBidi(), parsed.isBidi());
  }
}
