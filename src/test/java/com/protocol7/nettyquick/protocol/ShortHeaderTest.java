package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.frames.PingFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ShortHeaderTest {

  public static final byte[] DATA = "hello".getBytes();

  @Test
  public void roundtrip() {
    PacketType packetType = PacketType.Four_octets;
    ConnectionId connId = ConnectionId.random();
    PacketNumber pn = new PacketNumber(123);
    Payload payload = new Payload(new PingFrame(DATA));
    ShortHeader p = new ShortHeader(false,
                                    false,
                                    PacketType.Four_octets,
                                    Optional.of(connId),
                                    pn,
                                    payload);

    ByteBuf bb = Unpooled.buffer();
    p.write(bb);

    ShortHeader parsed = ShortHeader.parse(bb, connectionId -> new PacketNumber(122));

    assertFalse(parsed.isOmitConnectionId());
    assertFalse(parsed.isKeyPhase());
    assertEquals(packetType, parsed.getPacketType());
    assertEquals(Optional.of(connId), parsed.getDestinationConnectionId());
    assertEquals(pn, parsed.getPacketNumber());
    assertEquals(payload, parsed.getPayload());
  }

}