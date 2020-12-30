package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.*;

import com.protocol7.quincy.protocol.StreamId;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamFrameTest {

  public static final byte[] DATA = "Hello".getBytes();

  @Test
  public void roundtrip() {
    final StreamFrame frame = new StreamFrame(StreamId.next(-1, true, true), 0, true, DATA);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final StreamFrame parsed = StreamFrame.parse(bb);

    assertEquals(frame, parsed);
  }

  @Test
  public void parse() {
    final ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("10c67daf169411a94e48656c6c6f"));

    final StreamFrame frame = StreamFrame.parse(bb);

    assertFalse(frame.isFin());
    assertEquals(0, frame.getOffset());
    assertEquals(467722447824726350L, frame.getStreamId());
    assertArrayEquals(DATA, frame.getData());
  }

  @Test
  public void parseWithOffset() {
    final ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("14c2ebcf9abc5cbf78407b48656c6c6f"));

    final StreamFrame frame = StreamFrame.parse(bb);

    assertFalse(frame.isFin());
    assertEquals(123, frame.getOffset());
    assertEquals(210490071094968184L, frame.getStreamId());
    assertArrayEquals(DATA, frame.getData());
  }

  @Test
  public void parseWithFin() {
    final ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("13f9b7193a8d0827c40548656c6c6f"));

    final StreamFrame frame = StreamFrame.parse(bb);

    assertTrue(frame.isFin());
    assertEquals(0, frame.getOffset());
    assertEquals(4158820520164861892L, frame.getStreamId());
    assertArrayEquals(DATA, frame.getData());
  }

  @Test
  public void lengthWithoutOffset() {
    final StreamFrame frame = new StreamFrame(123, 0, false, DATA);
    assertEquals(9, frame.calculateLength());
  }

  @Test
  public void lengthWithOffset() {
    final StreamFrame frame = new StreamFrame(123, 123, false, DATA);
    assertEquals(11, frame.calculateLength());
  }
}
