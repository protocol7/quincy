package com.protocol7.nettyquic.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ConnectionCloseFrameTest {

  @Test
  public void parseKnown() {
    byte[] b = Hex.dehex("1c0019001b4e6f20726563656e74206e6574776f726b2061637469766974792e");
    ByteBuf bb = Unpooled.wrappedBuffer(b);

    ConnectionCloseFrame ccf = ConnectionCloseFrame.parse(bb);

    assertEquals(0x19, ccf.getErrorCode());
    assertEquals(FrameType.PADDING, ccf.getFrameType());
    assertEquals("No recent network activity.", ccf.getReasonPhrase());
  }

  @Test
  public void roundtripConnection() {
    ConnectionCloseFrame ccf = new ConnectionCloseFrame(12, FrameType.STREAM, "Hello world");

    ByteBuf bb = Unpooled.buffer();
    ccf.write(bb);

    ConnectionCloseFrame parsed = ConnectionCloseFrame.parse(bb);

    assertEquals(ccf.getErrorCode(), parsed.getErrorCode());
    assertEquals(ccf.getFrameType(), parsed.getFrameType());
    assertEquals(ccf.getReasonPhrase(), parsed.getReasonPhrase());
  }
}
