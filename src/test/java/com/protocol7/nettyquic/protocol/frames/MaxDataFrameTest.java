package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class MaxDataFrameTest {

  @Test
  public void roundtrip() {
    MaxDataFrame frame = new MaxDataFrame(456);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    MaxDataFrame parsed = MaxDataFrame.parse(bb);

    assertEquals(frame.getMaxData(), parsed.getMaxData());
  }
}
