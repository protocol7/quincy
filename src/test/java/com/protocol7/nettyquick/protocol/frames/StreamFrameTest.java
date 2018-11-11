package com.protocol7.nettyquick.protocol.frames;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamFrameTest {

  public static final byte[] DATA = "Hello".getBytes();

  @Test
  public void roundtrip() {
    StreamFrame frame = new StreamFrame(StreamId.random(true, true),
                                        0,
                                        true,
                                        DATA);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    StreamFrame parsed = StreamFrame.parse(bb);

    assertEquals(frame, parsed);
  }

  @Test
  public void parse() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("10c67daf169411a94e48656c6c6f"));

    StreamFrame frame = StreamFrame.parse(bb);

    assertFalse(frame.isFin());
    assertEquals(0, frame.getOffset());
    assertEquals(new StreamId(467722447824726350L), frame.getStreamId());
    assertArrayEquals(DATA, frame.getData());
  }

  @Test
  public void parseWithOffset() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("14c2ebcf9abc5cbf78407b48656c6c6f"));

    StreamFrame frame = StreamFrame.parse(bb);

    assertFalse(frame.isFin());
    assertEquals(123, frame.getOffset());
    assertEquals(new StreamId(210490071094968184L), frame.getStreamId());
    assertArrayEquals(DATA, frame.getData());
  }

  @Test
  public void parseWithFin() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("13f9b7193a8d0827c40548656c6c6f"));

    StreamFrame frame = StreamFrame.parse(bb);

    assertTrue(frame.isFin());
    assertEquals(0, frame.getOffset());
    assertEquals(new StreamId(4158820520164861892L), frame.getStreamId());
    assertArrayEquals(DATA, frame.getData());
  }

  @Test
  public void lengthWithoutOffset() {
    StreamFrame frame = new StreamFrame(new StreamId(123),
                                        0,
                                        false,
                                        DATA);
    assertEquals(9, frame.calculateLength());
  }

  @Test
  public void lengthWithOffset() {
    StreamFrame frame = new StreamFrame(new StreamId(123),
                                        123,
                                        false,
                                        DATA);
    assertEquals(11, frame.calculateLength());
  }
}