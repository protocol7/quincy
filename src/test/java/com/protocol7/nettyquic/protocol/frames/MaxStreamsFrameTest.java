package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class MaxStreamsFrameTest {

  @Test
  public void roundtrip() {
    MaxStreamsFrame frame = new MaxStreamsFrame(456);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    MaxStreamsFrame parsed = MaxStreamsFrame.parse(bb);

    assertEquals(frame.getMaxStreams(), parsed.getMaxStreams());
  }
}
