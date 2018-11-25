package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.TestAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.Test;

public class ShortHeaderTest {

  private final AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    ConnectionId connId = ConnectionId.random();
    PacketNumber pn = new PacketNumber(123);
    Payload payload = new Payload(PingFrame.INSTANCE);
    ShortHeader p = new ShortHeader(false, Optional.of(connId), pn, payload);

    ByteBuf bb = Unpooled.buffer();
    p.write(bb, aead);

    ShortHeader parsed = ShortHeader.parse(bb, c -> pn, (c, pt) -> aead, connId.getLength());

    assertFalse(parsed.isKeyPhase());
    assertEquals(Optional.of(connId), parsed.getDestinationConnectionId());
    assertEquals(pn, parsed.getPacketNumber());
    assertEquals(payload, parsed.getPayload());
  }
}
