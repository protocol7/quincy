package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.TestUtil;
import com.protocol7.nettyquick.protocol.frames.PaddingFrame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class UnprotectedPayloadTest {

  public static final byte[] DATA = "Hello".getBytes();

  @Test
  public void roundtrip() {
    UnprotectedPayload payload = new UnprotectedPayload(new PingFrame(DATA), PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb);

    UnprotectedPayload parsed = UnprotectedPayload.parse(bb, payload.getLength());

    assertEquals(payload, parsed);
  }

  @Test
  public void write() {
    UnprotectedPayload payload = new UnprotectedPayload(new PingFrame(DATA), PaddingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    payload.write(bb);

    TestUtil.assertBuffer("070548656c6c6f00", bb);
  }

  @Test
  public void parse() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("070548656c6c6f00"));
    UnprotectedPayload parsed = UnprotectedPayload.parse(bb, bb.writerIndex());

    UnprotectedPayload expected = new UnprotectedPayload(new PingFrame(DATA), PaddingFrame.INSTANCE);
    assertEquals(expected, parsed);
  }

  @Test
  public void addFrame() {
    UnprotectedPayload payload = new UnprotectedPayload(new PingFrame(DATA));

    UnprotectedPayload withAdded = payload.addFrame(PaddingFrame.INSTANCE);

    UnprotectedPayload expected = new UnprotectedPayload(new PingFrame(DATA), PaddingFrame.INSTANCE);

    assertEquals(expected, withAdded);
    assertEquals(new UnprotectedPayload(new PingFrame(DATA)), payload); // must not been mutated

  }
}