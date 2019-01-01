package com.protocol7.nettyquic.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.TestUtil;
import com.protocol7.nettyquic.protocol.frames.*;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.TestAEAD;
import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PayloadTest {

  private final AEAD aead = TestAEAD.create();
  private final PacketNumber pn = new PacketNumber(1);
  private final byte[] aad = new byte[12];

  @Test
  public void roundtrip() {
    Payload payload = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    Payload parsed = Payload.parse(bb, payload.calculateLength(), aead, pn, aad);

    assertEquals(payload, parsed);
  }

  @Test
  public void write() {
    Payload payload = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb, aead, pn, aad);

    TestUtil.assertBuffer("e1eca61dcd946af283d48c55a5d25967efd6", bb);
  }

  @Test
  public void parse() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("e1eca61dcd946af283d48c55a5d25967efd6"));
    Payload parsed = Payload.parse(bb, bb.writerIndex(), aead, pn, aad);

    Payload expected = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);
    assertEquals(expected, parsed);
  }

  @Test
  public void addFrame() {
    Payload payload = new Payload(PingFrame.INSTANCE);

    Payload withAdded = payload.addFrame(PaddingFrame.INSTANCE);

    Payload expected = new Payload(PingFrame.INSTANCE, PaddingFrame.INSTANCE);

    assertEquals(expected, withAdded);
    assertEquals(new Payload(PingFrame.INSTANCE), payload); // must not been mutated
  }
}
