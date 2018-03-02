package com.protocol7.nettyquick;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import org.junit.Assert;

public class TestUtil {

  public static void assertBuffer(byte[] expected, ByteBuf actual) {
    assertBuffer(Hex.hex(expected), actual);
  }

  public static void assertBuffer(String expected, ByteBuf actual) {
    byte[] actualBytes = new byte[actual.readableBytes()];
    actual.readBytes(actualBytes);
    assertEquals(expected, Hex.hex(actualBytes));
    assertBufferExhusted(actual);
  }

  public static void assertBufferExhusted(ByteBuf bb) {
    assertFalse(bb.isReadable());
  }
}
