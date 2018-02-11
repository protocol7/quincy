package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.*;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class VersionTest {

  @Test
  public void write() {
    ByteBuf bb = Unpooled.buffer();
    Version.DRAFT_09.write(bb);

    assertByteBuf(bb, "ff000009");
    assertByteBufExhusted(bb);
  }

  private void assertByteBuf(ByteBuf actual, String hexExpected) {
    assertByteBuf(actual, Hex.dehex(hexExpected));
  }

  private void assertByteBuf(ByteBuf actual, byte[] expected) {
    byte[] b = new byte[expected.length];
    actual.readBytes(b);
    assertArrayEquals(expected, b);
  }

  private void assertByteBufExhusted(ByteBuf bb) {
    assertFalse(bb.isReadable());
  }
}