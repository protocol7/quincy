package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PaddingFrameTest {

  @Test
  public void roundtrip() {
    final PaddingFrame frame = new PaddingFrame(10);

    final ByteBuf bb = Unpooled.buffer();

    frame.write(bb);

    final PaddingFrame parsed = PaddingFrame.parse(bb);

    assertEquals(frame, parsed);
  }

  @Test
  public void writeOne() {
    final PaddingFrame frame = new PaddingFrame(1);
    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    TestUtil.assertBuffer("00", bb);
  }

  @Test
  public void parseOne() {
    final PaddingFrame frame = PaddingFrame.parse(Unpooled.copiedBuffer(Hex.dehex("00")));

    assertEquals(new PaddingFrame(1), frame);
  }

  @Test
  public void parseMulti() {
    final PaddingFrame frame = PaddingFrame.parse(Unpooled.copiedBuffer(Hex.dehex("000000000000")));

    assertEquals(new PaddingFrame(6), frame);
  }

  @Test
  public void parseTrailing() {
    final ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("000000000000FF"));
    final PaddingFrame frame = PaddingFrame.parse(bb);

    assertEquals(new PaddingFrame(6), frame);
    assertEquals(1, bb.readableBytes());
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseIllegal() {
    PaddingFrame.parse(Unpooled.copiedBuffer(Hex.dehex("FF")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void cantBeEmpty() {
    new PaddingFrame(0);
  }

  @Test
  public void calculateLength() {
    assertEquals(10, new PaddingFrame(10).calculateLength());
  }
}
