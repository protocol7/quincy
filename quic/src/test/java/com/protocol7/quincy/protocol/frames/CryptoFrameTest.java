package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class CryptoFrameTest {

  @Test
  public void roundtrip() {
    CryptoFrame frame = new CryptoFrame(123, Hex.dehex("1234"));

    ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    CryptoFrame parsed = CryptoFrame.parse(bb);

    assertEquals(parsed.getOffset(), frame.getOffset());
    assertArrayEquals(parsed.getCryptoData(), frame.getCryptoData());
  }
}
