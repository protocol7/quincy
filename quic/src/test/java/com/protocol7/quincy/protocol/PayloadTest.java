package com.protocol7.quincy.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.protocol.frames.*;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PayloadTest {

  private final AEAD aead = TestAEAD.create();
  private final long pn = 1;
  private final byte[] aad = new byte[12];

  @Test
  public void roundtrip() {
    final Payload payload = new Payload(PingFrame.INSTANCE, new PaddingFrame(1));

    final ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    final Payload parsed = Payload.parse(bb, payload.calculateLength(), aead, pn, aad);

    assertEquals(payload, parsed);
  }

  @Test
  public void write() {
    final Payload payload = new Payload(PingFrame.INSTANCE, new PaddingFrame(1));

    final ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    TestUtil.assertBuffer("e1eca61dcd946af283d48c55a5d25967efd6", bb);
  }

  @Test
  public void parse() {
    final ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("e1eca61dcd946af283d48c55a5d25967efd6"));
    final Payload parsed = Payload.parse(bb, bb.writerIndex(), aead, pn, aad);

    final Payload expected = new Payload(PingFrame.INSTANCE, new PaddingFrame(1));
    assertEquals(expected, parsed);
  }

  @Test
  public void addFrame() {
    final Payload payload = new Payload(PingFrame.INSTANCE);

    final Payload withAdded = payload.addFrame(new PaddingFrame(1));

    final Payload expected = new Payload(PingFrame.INSTANCE, new PaddingFrame(1));

    assertEquals(expected, withAdded);
    assertEquals(new Payload(PingFrame.INSTANCE), payload); // must not been mutated
  }
}
