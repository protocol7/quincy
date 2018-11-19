package com.protocol7.nettyquick;

import io.netty.buffer.ByteBuf;

import static com.protocol7.nettyquick.utils.Hex.hex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestUtil {

  public static void assertBuffer(byte[] expected, ByteBuf actual) {
    assertBuffer(hex(expected), actual);
  }

  public static void assertBuffer(String expected, ByteBuf actual) {
    byte[] actualBytes = new byte[actual.readableBytes()];
    actual.readBytes(actualBytes);
    assertEquals(expected, hex(actualBytes));
    assertBufferExhusted(actual);
  }

  public static void assertBufferExhusted(ByteBuf bb) {
    assertFalse(bb.isReadable());
  }

  public static void assertHex(String expectedHex, byte[] actual) {
    assertEquals(expectedHex, hex(actual));
  }

  public static void assertHex(byte[] expected, byte[] actual) {
    assertEquals(hex(expected), hex(actual));
  }
}
