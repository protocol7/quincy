package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.tls.AEAD;
import com.protocol7.nettyquick.tls.NullAEAD;
import com.protocol7.nettyquick.tls.TestAEAD;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Optional;

import static java.util.Optional.empty;
import static org.junit.Assert.assertTrue;

public class PacketTest {

    private final AEAD aead = TestAEAD.create();
    private final ConnectionId connId = ConnectionId.random();
    private final PacketNumber pn = PacketNumber.random();

    @Test
    public void parseInitialPacket() {
        InitialPacket packet = InitialPacket.create(
                Optional.ofNullable(connId),
                empty(),
                empty(),
                Lists.newArrayList(PingFrame.INSTANCE));
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb, aead);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random(), c -> aead);
        assertTrue(parsed instanceof InitialPacket);
    }

    @Test
    public void parseVerNegPacket() {
        VersionNegotiationPacket packet = new VersionNegotiationPacket(empty(), empty(), Version.DRAFT_15);
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb, aead);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random(), c -> aead);
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

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random(), c -> aead);
        assertTrue(parsed instanceof VersionNegotiationPacket);
    }

    @Test
    public void parseHandshakePacket() {
        HandshakePacket packet = HandshakePacket.create(Optional.ofNullable(connId),
                empty(),
                pn,
                Version.CURRENT,
                PingFrame.INSTANCE);
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb, aead);

        Packet parsed = Packet.parse(bb, c -> pn, c -> aead);
        assertTrue(parsed instanceof HandshakePacket);
    }

    @Test
    public void parseRetryPacket() {
        RetryPacket packet = new RetryPacket(Version.CURRENT,
                Optional.ofNullable(connId),
                empty(),
                connId,
                Rnd.rndBytes(11));
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb, aead);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random(), c -> aead);
        assertTrue(parsed instanceof RetryPacket);
    }

    @Test
    public void parseShortPacket() {
        ShortPacket packet = new ShortPacket(new ShortHeader(
                false,
                Optional.of(connId),
                pn,
                new ProtectedPayload()));
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb, aead);

        Packet parsed = Packet.parse(bb, c -> pn, c -> aead);
        assertTrue(parsed instanceof ShortPacket);
    }

    @Test
    public void name() {
        byte[] b = Hex.dehex("ff000000650188f9f1ab0040b58001a7f5d3fc96f5e977ac558a8379f56de1a7e99f962602608df4f59e277eaa0228fd0a2a834fbee439ba30f5e84119dd41252056502e6ad698a70c3a10c971aaba0f5a59d8b05b003a7735abc255bf71d198791baf993112a0e8ce50611f14a35c3371c3f044e46951904f0ff4de6229748e3bedc72a3382e3536510240d647d4309cc5251a54e95fb868d5584be6468f25c706e3ff7fa1a719ff23225518743565f4b3578b4d513994021af3f7a6e607fe615b057c3");

        // server 	Long Header{Type: Initial, DestConnectionID: (empty), SrcConnectionID: 0x88f9f1ab, Token: (empty), PacketNumber: 0x1, PacketNumberLen: 2, PayloadLen: 181, Version: TLS dev version (WIP)}

        ByteBuf bb = Unpooled.wrappedBuffer(b);

        Packet.parse(bb, c -> pn, c -> aead);
    }
}