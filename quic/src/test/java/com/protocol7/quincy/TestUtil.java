package com.protocol7.quincy;

import static com.protocol7.quincy.utils.Hex.hex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;

public class TestUtil {

  public static void assertBuffer(final byte[] expected, final ByteBuf actual) {
    assertBuffer(hex(expected), actual);
  }

  public static void assertBuffer(final String expected, final ByteBuf actual) {
    final byte[] actualBytes = new byte[actual.readableBytes()];
    actual.readBytes(actualBytes);
    assertEquals(expected, hex(actualBytes));
    assertBufferExhusted(actual);
  }

  public static void assertBufferExhusted(final ByteBuf bb) {
    assertFalse(bb.isReadable());
  }

  public static void assertHex(final String expectedHex, final byte[] actual) {
    assertEquals(expectedHex, hex(actual));
  }

  public static void assertHex(final String expectedHex, final ByteBuf actual) {
    final byte[] actualBytes = Bytes.peekToArray(actual);
    assertHex(expectedHex, actualBytes);
  }

  public static void assertHex(final byte[] expected, final byte[] actual) {
    assertEquals(hex(expected), hex(actual));
  }

  public static InetSocketAddress getTestAddress() {
    return new InetSocketAddress("127.0.0.1", 4444);
  }
}
