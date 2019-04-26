package com.protocol7.quincy.protocol.packets;

import static java.util.Optional.empty;
import static org.junit.Assert.assertTrue;

import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.Test;

public class PacketTest {

  private final AEAD aead = TestAEAD.create();
  private final ConnectionId connId = ConnectionId.random();
  private final PacketNumber pn = new PacketNumber(123);

  @Test
  public void parseInitialPacket() {
    final InitialPacket packet =
        InitialPacket.create(
            Optional.ofNullable(connId),
            empty(),
            pn,
            Version.DRAFT_18,
            empty(),
            new PaddingFrame(1));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof InitialPacket);
  }

  @Test
  public void parseVerNegPacket() {
    final VersionNegotiationPacket packet =
        new VersionNegotiationPacket(empty(), empty(), Version.DRAFT_15);
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof VersionNegotiationPacket);
  }

  @Test
  public void parseVerNegPacketWithClashingMarker() {
    // even if the market byte matches a different packet, anything with 0 version must be a ver neg
    // packet
    // craft a special ver neg packet
    final ByteBuf bb = Unpooled.buffer();
    final int b = (0b10000000 | PacketType.Initial.getType() << 4) & 0xFF;
    bb.writeByte(b);
    Version.VERSION_NEGOTIATION.write(bb);
    bb.writeByte(0);
    Version.DRAFT_15.write(bb);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof VersionNegotiationPacket);
  }

  @Test
  public void parseHandshakePacket() {
    final HandshakePacket packet =
        HandshakePacket.create(
            Optional.ofNullable(connId), empty(), pn, Version.DRAFT_18, new PaddingFrame(1));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof HandshakePacket);
  }

  @Test
  public void parseRetryPacket() {
    final RetryPacket packet =
        new RetryPacket(
            Version.DRAFT_18, Optional.ofNullable(connId), empty(), connId, Rnd.rndBytes(11));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof RetryPacket);
  }

  @Test
  public void parseShortPacket() {
    final ShortPacket packet =
        new ShortPacket(false, Optional.of(connId), pn, new Payload(PingFrame.INSTANCE));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof ShortPacket);
  }
}
