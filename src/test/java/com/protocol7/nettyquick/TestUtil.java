package com.protocol7.nettyquick;

import static org.junit.Assert.assertFalse;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import org.junit.Assert;

public class TestUtil {

  public static void assertBuffer(String expected, ByteBuf actual) {
    assertBuffer(Hex.dehex(expected), actual);
  }

  public static void assertBuffer(byte[] expected, ByteBuf actual) {
    byte[] actualBytes = new byte[expected.length];
    actual.readBytes(actualBytes);
    Assert.assertArrayEquals(actualBytes, expected);
    assertBufferExhusted(actual);
  }

  public static void assertBufferExhusted(ByteBuf bb) {
    assertFalse(bb.isReadable());
  }
}
