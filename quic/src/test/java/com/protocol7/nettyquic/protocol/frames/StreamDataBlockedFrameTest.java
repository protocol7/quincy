package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamDataBlockedFrameTest {

  @Test
  public void roundtrip() {
    StreamDataBlockedFrame frame = new StreamDataBlockedFrame(new StreamId(123), 456);

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    StreamDataBlockedFrame parsed = StreamDataBlockedFrame.parse(bb);

    assertEquals(frame.getStreamId(), parsed.getStreamId());
    assertEquals(frame.getStreamDataLimit(), parsed.getStreamDataLimit());
  }
}
