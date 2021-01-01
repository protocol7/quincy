package com.protocol7.quincy.protocol.packets;

import static com.protocol7.quincy.protocol.ConnectionId.EMPTY;
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
import org.junit.Test;

public class PacketTest {

  private final AEAD aead = TestAEAD.create();
  private final ConnectionId connId = ConnectionId.random();
  private final long pn = 123;

  @Test
  public void parseInitialPacket() {
    final InitialPacket packet =
        InitialPacket.create(connId, EMPTY, pn, Version.DRAFT_29, empty(), new PaddingFrame(6));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof InitialPacket);
  }

  @Test
  public void parseVerNegPacket() {
    final VersionNegotiationPacket packet =
        new VersionNegotiationPacket(EMPTY, EMPTY, Version.DRAFT_29);
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
    ConnectionId.write(EMPTY, bb);
    ConnectionId.write(EMPTY, bb);
    Version.DRAFT_29.write(bb);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof VersionNegotiationPacket);
  }

  @Test
  public void parseHandshakePacket() {
    final HandshakePacket packet =
        HandshakePacket.create(connId, EMPTY, pn, Version.DRAFT_29, new PaddingFrame(6));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof HandshakePacket);
  }

  @Test
  public void parseRetryPacket() {
    final RetryPacket packet =
        RetryPacket.createOutgoing(Version.DRAFT_29, connId, EMPTY, connId, Rnd.rndBytes(11));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof RetryPacket);
  }

  @Test
  public void parseShortPacket() {
    final ShortPacket packet = new ShortPacket(false, connId, pn, new Payload(PingFrame.INSTANCE));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    final Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof ShortPacket);
  }
}
