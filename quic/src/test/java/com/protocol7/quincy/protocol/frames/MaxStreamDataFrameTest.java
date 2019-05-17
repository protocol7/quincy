package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class MaxStreamDataFrameTest {

  @Test
  public void roundtrip() {
    final MaxStreamDataFrame frame = new MaxStreamDataFrame(123, 456);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final MaxStreamDataFrame parsed = MaxStreamDataFrame.parse(bb);

    assertEquals(frame.getStreamId(), parsed.getStreamId());
    assertEquals(frame.getMaxStreamData(), parsed.getMaxStreamData());
  }
}
