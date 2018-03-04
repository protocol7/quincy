package com.protocol7.nettyquick.protocol.frames;

import static org.junit.Assert.*;

import com.protocol7.nettyquick.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class RstStreamFrameTest {

  @Test(expected = NullPointerException.class)
  public void nullStreamId() {
    new RstStreamFrame(null, 123, 456);
  }

  @Test
  public void validAppErrorCode() {
    new RstStreamFrame(StreamId.random(), 0, 456);
    new RstStreamFrame(StreamId.random(), 123, 456);
    new RstStreamFrame(StreamId.random(), 0xFFFF, 456);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeAppErrorCode() {
    new RstStreamFrame(StreamId.random(), -1, 456);
  }
  @Test(expected = IllegalArgumentException.class)
  public void tooLargeAppErrorCode() {
    new RstStreamFrame(StreamId.random(), 0xFFFF+1, 456);
  }

  @Test
  public void rountrip() {
    StreamId streamId = StreamId.random();
    RstStreamFrame frame = new RstStreamFrame(streamId, 123, 456);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    RstStreamFrame parsed = RstStreamFrame.parse(bb);

    assertEquals(frame, parsed);
  }
}