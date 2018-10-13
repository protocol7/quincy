package com.protocol7.nettyquick.protocol.frames;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.List;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class AckFrameTest {

  @Test
  public void roundtrip() {
    List<AckBlock> blocks = Lists.newArrayList(AckBlock.fromLongs(1, 5), AckBlock.fromLongs(7, 8), AckBlock.fromLongs(12, 100));
    AckFrame frame = new AckFrame(1234, blocks);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    AckFrame parsed = AckFrame.parse(bb);

    assertEquals(frame.getAckDelay(), parsed.getAckDelay());
    assertEquals(frame.getBlocks(), parsed.getBlocks());
  }

  @Test
  public void roundtripSinglePacket() {
    List<AckBlock> blocks = Lists.newArrayList(AckBlock.fromLongs(100, 100));
    AckFrame frame = new AckFrame(1234, blocks);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    AckFrame parsed = AckFrame.parse(bb);

    assertEquals(frame.getAckDelay(), parsed.getAckDelay());
    assertEquals(frame.getBlocks(), parsed.getBlocks());
  }

  @Test
  public void writeSinglePacket() {
    List<AckBlock> blocks = Lists.newArrayList(AckBlock.fromLongs(100, 100));
    AckFrame frame = new AckFrame(1234, blocks);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    ByteBuf expected = Unpooled.copiedBuffer(Hex.dehex("1a406444d20000"));
    assertEquals(expected, bb);
  }
}