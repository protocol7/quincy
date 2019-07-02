package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ConnectionCloseFrameTest {

  @Test
  public void parseKnown() {
    final byte[] b = Hex.dehex("1c19001b4e6f20726563656e74206e6574776f726b2061637469766974792e");
    final ByteBuf bb = Unpooled.wrappedBuffer(b);

    final ConnectionCloseFrame ccf = ConnectionCloseFrame.parse(bb);

    assertEquals(0x19, ccf.getErrorCode());
    assertEquals(FrameType.PADDING, ccf.getFrameType());
    assertEquals("No recent network activity.", ccf.getReasonPhrase());
  }

  @Test
  public void roundtripConnection() {
    final ConnectionCloseFrame ccf = new ConnectionCloseFrame(12, FrameType.STREAM, "Hello world");

    final ByteBuf bb = Unpooled.buffer();
    ccf.write(bb);

    final ConnectionCloseFrame parsed = ConnectionCloseFrame.parse(bb);

    assertEquals(ccf.getErrorCode(), parsed.getErrorCode());
    assertEquals(ccf.getFrameType(), parsed.getFrameType());
    assertEquals(ccf.getReasonPhrase(), parsed.getReasonPhrase());
  }
}
