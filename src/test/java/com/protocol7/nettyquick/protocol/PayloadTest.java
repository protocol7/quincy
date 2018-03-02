package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.TestUtil;
import com.protocol7.nettyquick.protocol.frames.PaddingFrame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PayloadTest {

  public static final byte[] DATA = "Hello".getBytes();

  @Test
  public void roundtrip() {
    Payload payload = new Payload(new PingFrame(DATA), PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb);

    Payload parsed = Payload.parse(bb);

    assertEquals(payload, parsed);
  }

  @Test
  public void write() {
    Payload payload = new Payload(new PingFrame(DATA), PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb);

    TestUtil.assertBuffer("070548656c6c6f00", bb);
  }

  @Test
  public void parse() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("070548656c6c6f00"));
    Payload parsed = Payload.parse(bb);

    Payload expected = new Payload(new PingFrame(DATA), PaddingFrame.INSTANCE);
    assertEquals(expected, parsed);
  }

  @Test
  public void addFrame() {
    Payload payload = new Payload(new PingFrame(DATA));

    Payload withAdded = payload.addFrame(PaddingFrame.INSTANCE);

    Payload expected = new Payload(new PingFrame(DATA), PaddingFrame.INSTANCE);

    assertEquals(expected, withAdded);
    assertEquals(new Payload(new PingFrame(DATA)), payload); // must not been mutated

  }
}