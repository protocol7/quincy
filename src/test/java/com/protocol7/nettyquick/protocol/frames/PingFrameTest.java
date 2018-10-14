package com.protocol7.nettyquick.protocol.frames;

import com.protocol7.nettyquick.TestUtil;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PingFrameTest {

  public static final byte[] DATA = "hello".getBytes();

  @Test
  public void roundtrip() {
    ByteBuf bb = Unpooled.buffer();
    PingFrame frame = PingFrame.INSTANCE;

    frame.write(bb);

    PingFrame parsed = PingFrame.parse(bb);

    assertEquals(frame, parsed);
  }

  @Test
  public void write() {
    ByteBuf bb = Unpooled.buffer();
    PingFrame frame = PingFrame.INSTANCE;
    frame.write(bb);

    TestUtil.assertBuffer("07", bb);
  }
}