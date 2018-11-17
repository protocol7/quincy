package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.TestAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class LongHeaderTest {

  private final AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    PacketType packetType = PacketType.Initial;
    Optional<ConnectionId> destConnId = Optional.of(ConnectionId.random());
    Optional<ConnectionId> srcConnId = Optional.of(ConnectionId.random());
    PacketNumber pn = PacketNumber.random();
    Version version = Version.CURRENT;
    Payload payload = new Payload(PingFrame.INSTANCE);

    LongHeader lp = new LongHeader(packetType, destConnId, srcConnId, version, pn, payload);

    ByteBuf bb = Unpooled.buffer();
    lp.write(bb, aead);
    LongHeader parsed = LongHeader.parse(bb, true, (c, p) -> aead);

    assertEquals(packetType, parsed.getPacketType());
    assertEquals(destConnId, parsed.getDestinationConnectionId());
    assertEquals(srcConnId, parsed.getSourceConnectionId());
    assertEquals(version, parsed.getVersion());
    assertEquals(pn, parsed.getPacketNumber());
    assertEquals(payload, parsed.getPayload());
  }


}