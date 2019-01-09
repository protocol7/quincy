package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamsBlockedFrameTest {

  @Test
  public void roundtrip() {
    StreamsBlockedFrame frame = new StreamsBlockedFrame(456);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    StreamsBlockedFrame parsed = StreamsBlockedFrame.parse(bb);

    assertEquals(frame.getStreamsLimit(), parsed.getStreamsLimit());
  }
}
