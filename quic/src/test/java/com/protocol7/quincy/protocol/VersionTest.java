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
    Version.DRAFT_15.write(bb);

    TestUtil.assertBuffer("ff00000f", bb);
    TestUtil.assertBufferExhusted(bb);
  }

  @Test
  public void writeQuicGo() {
    final ByteBuf bb = Unpooled.buffer();
    Version.QUIC_GO.write(bb);

    TestUtil.assertBuffer("51474fff", bb);
    TestUtil.assertBufferExhusted(bb);
  }

  @Test
  public void read() {
    assertEquals(Version.QUIC_GO, Version.read(b("51474fff")));
    assertEquals(Version.DRAFT_17, Version.read(b("ff000011")));
    assertEquals(Version.DRAFT_18, Version.read(b("ff000012")));
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
    Version.DRAFT_15.write(bb);

    final Version parsed = Version.read(bb);

    assertEquals(Version.DRAFT_15, parsed);
  }
}
