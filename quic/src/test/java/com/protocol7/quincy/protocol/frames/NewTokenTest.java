package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.*;

import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class NewTokenTest {

  @Test
  public void roundtrip() {
    final NewToken token = new NewToken(Rnd.rndBytes(20));

    final ByteBuf bb = Unpooled.buffer();

    token.write(bb);

    final NewToken parsed = NewToken.parse(bb);

    assertArrayEquals(token.getToken(), parsed.getToken());
  }
}
