package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertSame;

import com.protocol7.nettyquic.TestUtil;
import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PaddingFrameTest {

  @Test
  public void roundtrip() {
    PaddingFrame frame = PaddingFrame.INSTANCE;

    ByteBuf bb = Unpooled.buffer();

    frame.write(bb);

    PaddingFrame parsed = PaddingFrame.parse(bb);

    assertSame(frame, parsed);
  }

  @Test
  public void write() {
    PaddingFrame frame = PaddingFrame.INSTANCE;
    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    TestUtil.assertBuffer("00", bb);
  }

  @Test
  public void parse() {
    PaddingFrame frame = PaddingFrame.parse(Unpooled.copiedBuffer(Hex.dehex("00")));

    assertSame(PaddingFrame.INSTANCE, frame);
  }
}
