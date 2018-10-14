package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.protocol.frames.PingFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Optional;

public class LongHeaderTest {

  @Test
  public void roundtrip() {
    PacketType packetType = PacketType.Initial;
    Optional<ConnectionId> destConnId = Optional.of(ConnectionId.random());
    Optional<ConnectionId> srcConnId = Optional.of(ConnectionId.random());
    PacketNumber pn = PacketNumber.random();
    Version version = Version.CURRENT;
    UnprotectedPayload payload = new UnprotectedPayload(PingFrame.INSTANCE);

    LongHeader lp = new LongHeader(packetType, destConnId, srcConnId, version, pn, payload);

    ByteBuf bb = Unpooled.buffer();
    lp.write(bb);
    LongHeader parsed = LongHeader.parse(bb);

    assertEquals(packetType, parsed.getPacketType());
    assertEquals(destConnId, parsed.getDestinationConnectionId());
    assertEquals(srcConnId, parsed.getSourceConnectionId());
    assertEquals(version, parsed.getVersion());
    assertEquals(pn, parsed.getPacketNumber());
    assertEquals(payload, parsed.getPayload());
  }


}