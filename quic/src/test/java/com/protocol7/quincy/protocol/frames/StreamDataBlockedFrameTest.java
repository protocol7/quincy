package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamDataBlockedFrameTest {

  @Test
  public void roundtrip() {
    final StreamDataBlockedFrame frame = new StreamDataBlockedFrame(new StreamId(123), 456);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final StreamDataBlockedFrame parsed = StreamDataBlockedFrame.parse(bb);

    assertEquals(frame.getStreamId(), parsed.getStreamId());
    assertEquals(frame.getStreamDataLimit(), parsed.getStreamDataLimit());
  }
}
