package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ApplicationCloseFrameTest {

  @Test
  public void roundtripApplication() {
    ApplicationCloseFrame acf = new ApplicationCloseFrame(12, "Hello world");

    ByteBuf bb = Unpooled.buffer();
    acf.write(bb);

    ApplicationCloseFrame parsed = ApplicationCloseFrame.parse(bb);

    assertEquals(acf.getErrorCode(), parsed.getErrorCode());
    assertEquals(acf.getReasonPhrase(), parsed.getReasonPhrase());
  }
}
