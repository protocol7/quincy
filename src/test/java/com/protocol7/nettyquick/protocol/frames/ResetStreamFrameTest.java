package com.protocol7.nettyquick.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ResetStreamFrameTest {

  @Test(expected = NullPointerException.class)
  public void nullStreamId() {
    new ResetStreamFrame(null, 123, 456);
  }

  @Test
  public void validAppErrorCode() {
    new ResetStreamFrame(StreamId.random(true, true), 0, 456);
    new ResetStreamFrame(StreamId.random(true, true), 123, 456);
    new ResetStreamFrame(StreamId.random(true, true), 0xFFFF, 456);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeAppErrorCode() {
    new ResetStreamFrame(StreamId.random(true, true), -1, 456);
  }

  @Test(expected = IllegalArgumentException.class)
  public void tooLargeAppErrorCode() {
    new ResetStreamFrame(StreamId.random(true, true), 0xFFFF + 1, 456);
  }

  @Test
  public void rountrip() {
    StreamId streamId = StreamId.random(true, true);
    ResetStreamFrame frame = new ResetStreamFrame(streamId, 123, 456);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    ResetStreamFrame parsed = ResetStreamFrame.parse(bb);

    assertEquals(frame, parsed);
  }
}
