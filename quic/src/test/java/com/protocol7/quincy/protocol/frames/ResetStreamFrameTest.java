package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ResetStreamFrameTest {

  @Test(expected = IllegalArgumentException.class)
  public void invalidStreamId() {
    new ResetStreamFrame(-1, 123, 456);
  }

  @Test
  public void validAppErrorCode() {
    new ResetStreamFrame(StreamId.next(-1, true, true), 0, 456);
    new ResetStreamFrame(StreamId.next(-1, true, true), 123, 456);
    new ResetStreamFrame(StreamId.next(-1, true, true), 0xFFFF, 456);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeAppErrorCode() {
    new ResetStreamFrame(StreamId.next(-1, true, true), -1, 456);
  }

  @Test(expected = IllegalArgumentException.class)
  public void tooLargeAppErrorCode() {
    new ResetStreamFrame(StreamId.next(-1, true, true), 0xFFFF + 1, 456);
  }

  @Test
  public void rountrip() {
    final long streamId = StreamId.next(-1, true, true);
    final ResetStreamFrame frame = new ResetStreamFrame(streamId, 123, 456);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final ResetStreamFrame parsed = ResetStreamFrame.parse(bb);

    assertEquals(frame, parsed);
  }
}
