package com.protocol7.quincy.protocol.packets;

import static java.util.Optional.empty;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class InitialPacketTest {

  private ConnectionId destConnId = ConnectionId.random();
  private ConnectionId srcConnId = ConnectionId.random();
  private byte[] token = Rnd.rndBytes(16);

  private final AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    final InitialPacket packet = p(new PacketNumber(123), Optional.of(token));

    final ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    final InitialPacket parsed = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertEquals(srcConnId, parsed.getSourceConnectionId().get());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertArrayEquals(token, parsed.getToken().get());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
  }

  @Test
  public void roundtripMultiple() {
    final ByteBuf bb = Unpooled.buffer();

    p(new PacketNumber(1), Optional.of(token)).write(bb, aead);
    p(new PacketNumber(2), Optional.of(token)).write(bb, aead);

    final InitialPacket parsed1 = InitialPacket.parse(bb).complete(l -> aead);
    final InitialPacket parsed2 = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(parsed1.getDestinationConnectionId(), parsed2.getDestinationConnectionId());
    assertEquals(parsed1.getSourceConnectionId(), parsed2.getSourceConnectionId());
    assertEquals(new PacketNumber(1), parsed1.getPacketNumber());
    assertEquals(new PacketNumber(2), parsed2.getPacketNumber());
    assertEquals(parsed1.getVersion(), parsed2.getVersion());
    assertEquals(parsed1.getPayload(), parsed2.getPayload());
  }

  @Test
  public void roundtripNoToken() {
    final InitialPacket packet = p(new PacketNumber(123), empty());

    final ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    final InitialPacket parsed = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertEquals(srcConnId, parsed.getSourceConnectionId().get());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertFalse(parsed.getToken().isPresent());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
  }

  private InitialPacket p(final PacketNumber pn, final Optional<byte[]> token) {
    return InitialPacket.create(
        Optional.ofNullable(destConnId),
        Optional.ofNullable(srcConnId),
        pn,
        Version.DRAFT_18,
        token,
        List.of(PingFrame.INSTANCE));
  }
}
