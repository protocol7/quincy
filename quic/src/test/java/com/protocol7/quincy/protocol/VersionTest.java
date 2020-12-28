package com.protocol7.quincy.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class VersionTest {

  @Test
  public void write() {
    final ByteBuf bb = Unpooled.buffer();
    Version.DRAFT_29.write(bb);

    TestUtil.assertBuffer("ff00001d", bb);
    TestUtil.assertBufferExhusted(bb);
  }

  @Test
  public void read() {
    assertEquals(Version.DRAFT_29, Version.read(b("ff00001d")));
    assertEquals(Version.FINAL, Version.read(b("00000001")));
    assertEquals(Version.VERSION_NEGOTIATION, Version.read(b("00000000")));
    assertEquals(Version.UNKNOWN, Version.read(b("abcdabcd")));
  }

  private ByteBuf b(final String d) {
    final ByteBuf bb = Unpooled.buffer();
    bb.writeBytes(Hex.dehex(d));
    return bb;
  }

  @Test
  public void roundtrip() {
    final ByteBuf bb = Unpooled.buffer();
    Version.DRAFT_29.write(bb);

    final Version parsed = Version.read(bb);

    assertEquals(Version.DRAFT_29, parsed);
  }
}
