package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Optional;

import static java.util.Optional.empty;
import static org.junit.Assert.assertTrue;

public class PacketTest {

    @Test
    public void parseInitialPacket() {
        InitialPacket packet = InitialPacket.create(empty(),
                empty(),
                empty(),
                Lists.newArrayList(PingFrame.INSTANCE));
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof InitialPacket);
    }

    @Test
    public void parseVerNegPacket() {
        VersionNegotiationPacket packet = new VersionNegotiationPacket(empty(), empty(), Version.DRAFT_15);
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof VersionNegotiationPacket);
    }

    @Test
    public void parseVerNegPacketWithClashingMarker() {
        // even if the market byte matches a different packet, anything with 0 version must be a ver neg packet
        // craft a special ver neg packet
        ByteBuf bb = Unpooled.buffer();
        bb.writeByte(InitialPacket.MARKER);
        Version.VERSION_NEGOTIATION.write(bb);
        bb.writeByte(0);
        Version.DRAFT_15.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof VersionNegotiationPacket);
    }

    @Test
    public void parseHandshakePacket() {
        HandshakePacket packet = HandshakePacket.create(empty(), empty(), PacketNumber.random(), Version.CURRENT, PingFrame.INSTANCE);
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof HandshakePacket);
    }

    @Test
    public void parseRetryPacket() {
        RetryPacket packet = new RetryPacket(Version.CURRENT, empty(), empty(), ConnectionId.random(), Rnd.rndBytes(11));
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof RetryPacket);
    }

    @Test
    public void parseShortPacket() {
        ShortPacket packet = new ShortPacket(new ShortHeader(
                false,
                Optional.of(ConnectionId.random()),
                PacketNumber.random(),
                new ProtectedPayload()));
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof ShortPacket);
    }
}