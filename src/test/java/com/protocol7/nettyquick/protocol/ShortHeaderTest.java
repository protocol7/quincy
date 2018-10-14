package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.frames.PingFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ShortHeaderTest {

  @Test
  public void roundtrip() {
    ConnectionId connId = ConnectionId.random();
    PacketNumber pn = new PacketNumber(123);
    ProtectedPayload payload = new ProtectedPayload(PingFrame.INSTANCE);
    ShortHeader p = new ShortHeader(false,
                                    Optional.of(connId),
                                    pn,
                                    payload);

    ByteBuf bb = Unpooled.buffer();
    p.write(bb);

    ShortHeader parsed = ShortHeader.parse(bb, connectionId -> new PacketNumber(122));

    assertFalse(parsed.isKeyPhase());
    assertEquals(Optional.of(connId), parsed.getDestinationConnectionId());
    assertEquals(pn, parsed.getPacketNumber());
    // assertEquals(payload, parsed.getPayload()); // TODO enable when parsing protected payloads correctly
  }

}