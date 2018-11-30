package com.protocol7.nettyquick.protocol.packets;

import static java.util.Optional.empty;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.TestAEAD;
import com.protocol7.nettyquick.utils.Rnd;
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
    InitialPacket packet =
        InitialPacket.create(
            Optional.ofNullable(connId),
            empty(),
            pn,
            Version.CURRENT,
            empty(),
            Lists.newArrayList(PingFrame.INSTANCE));
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof InitialPacket);
  }

  @Test
  public void parseVerNegPacket() {
    VersionNegotiationPacket packet =
        new VersionNegotiationPacket(empty(), empty(), Version.DRAFT_15);
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof VersionNegotiationPacket);
  }

  @Test
  public void parseVerNegPacketWithClashingMarker() {
    // even if the market byte matches a different packet, anything with 0 version must be a ver neg
    // packet
    // craft a special ver neg packet
    ByteBuf bb = Unpooled.buffer();
    bb.writeByte(InitialPacket.MARKER);
    Version.VERSION_NEGOTIATION.write(bb);
    bb.writeByte(0);
    Version.DRAFT_15.write(bb);

    Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof VersionNegotiationPacket);
  }

  @Test
  public void parseHandshakePacket() {
    HandshakePacket packet =
        HandshakePacket.create(
            Optional.ofNullable(connId), empty(), pn, Version.CURRENT, PingFrame.INSTANCE);
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof HandshakePacket);
  }

  @Test
  public void parseRetryPacket() {
    RetryPacket packet =
        new RetryPacket(
            Version.CURRENT, Optional.ofNullable(connId), empty(), connId, Rnd.rndBytes(11));
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof RetryPacket);
  }

  @Test
  public void parseShortPacket() {
    ShortPacket packet =
        new ShortPacket(
            new ShortHeader(false, Optional.of(connId), pn, new Payload(PingFrame.INSTANCE)));
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    Packet parsed = Packet.parse(bb, connId.getLength()).complete(l -> aead);
    assertTrue(parsed instanceof ShortPacket);
  }
}
