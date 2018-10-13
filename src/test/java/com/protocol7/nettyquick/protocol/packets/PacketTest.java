package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.junit.Assert.assertTrue;

public class PacketTest {

    @Test
    public void parseInitialPacket() {
        InitialPacket packet = InitialPacket.create(empty(),
                empty(),
                empty(),
                emptyList());
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof InitialPacket);
    }

    @Test
    public void parseHandshakePacket() {
        HandshakePacket packet = HandshakePacket.create(empty(), empty(), PacketNumber.random(), Version.CURRENT);
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        Packet parsed = Packet.parse(bb, c -> PacketNumber.random());
        assertTrue(parsed instanceof HandshakePacket);
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