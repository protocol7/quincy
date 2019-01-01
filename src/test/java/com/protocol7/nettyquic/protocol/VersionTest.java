package com.protocol7.nettyquic.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.TestUtil;
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
  public void roundtrip() {
    ByteBuf bb = Unpooled.buffer();
    Version.DRAFT_15.write(bb);

    Version parsed = Version.read(bb);

    assertEquals(Version.DRAFT_15, parsed);
  }
}
