package com.protocol7.quincy.protocol.packets;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.Test;

public class HandshakePacketTest {

  private ConnectionId destConnId = ConnectionId.random();
  private ConnectionId srcConnId = ConnectionId.random();

  private final AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    final HandshakePacket packet = p(PacketNumber.MIN);

    final ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    final HandshakePacket parsed = HandshakePacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertEquals(srcConnId, parsed.getSourceConnectionId().get());
    assertEquals(PacketNumber.MIN, parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
  }

  @Test
  public void roundtripMultipePackets() {
    final ByteBuf bb = Unpooled.buffer();

    // write two packets to the same buffer
    p(new PacketNumber(1)).write(bb, aead);
    p(new PacketNumber(2)).write(bb, aead);

    // parse both packet
    final HandshakePacket parsed1 = HandshakePacket.parse(bb).complete(l -> aead);
    final HandshakePacket parsed2 = HandshakePacket.parse(bb).complete(l -> aead);

    assertEquals(parsed1.getDestinationConnectionId(), parsed2.getDestinationConnectionId());
    assertEquals(parsed1.getSourceConnectionId(), parsed2.getSourceConnectionId());
    assertEquals(parsed1.getVersion(), parsed2.getVersion());
    assertEquals(new PacketNumber(1), parsed1.getPacketNumber());
    assertEquals(new PacketNumber(2), parsed2.getPacketNumber());
    assertEquals(parsed1.getPayload(), parsed2.getPayload());
  }

  private HandshakePacket p(final PacketNumber pn) {
    return HandshakePacket.create(
        Optional.of(destConnId), Optional.of(srcConnId), pn, Version.DRAFT_18, PingFrame.INSTANCE);
  }
}
