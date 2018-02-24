package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

  @Test
  public void roundtrip() {
    ByteBuf bb = Unpooled.buffer();
    Version.DRAFT_09.write(bb);

    Version parsed = Version.read(bb);

    assertEquals(Version.DRAFT_09, parsed);
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