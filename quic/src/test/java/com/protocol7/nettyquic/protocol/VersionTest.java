package com.protocol7.nettyquic.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.TestUtil;
import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class VersionTest {

  @Test
  public void write() {
    ByteBuf bb = Unpooled.buffer();
    Version.DRAFT_15.write(bb);

    TestUtil.assertBuffer("ff00000f", bb);
    TestUtil.assertBufferExhusted(bb);
  }

  @Test
  public void writeQuicGo() {
    ByteBuf bb = Unpooled.buffer();
    Version.QUIC_GO.write(bb);

    TestUtil.assertBuffer("51474fff", bb);
    TestUtil.assertBufferExhusted(bb);
  }

  @Test
  public void read() {
    assertEquals(Version.QUIC_GO, Version.read(b("51474fff")));
    assertEquals(Version.DRAFT_17, Version.read(b("ff000011")));
    assertEquals(Version.FINAL, Version.read(b("00000001")));
    assertEquals(Version.VERSION_NEGOTIATION, Version.read(b("00000000")));
    assertEquals(Version.UNKNOWN, Version.read(b("abcdabcd")));
  }

  private ByteBuf b(String d) {
    ByteBuf bb = Unpooled.buffer();
    bb.writeBytes(Hex.dehex(d));
    return bb;
  }

  @Test
  public void roundtrip() {
    ByteBuf bb = Unpooled.buffer();
    Version.DRAFT_15.write(bb);

    Version parsed = Version.read(bb);

    assertEquals(Version.DRAFT_15, parsed);
  }
}
