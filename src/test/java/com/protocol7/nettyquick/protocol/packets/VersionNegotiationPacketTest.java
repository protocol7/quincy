package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Version;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.List;

import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class VersionNegotiationPacketTest {

    private ConnectionId dest = ConnectionId.random();
    private ConnectionId src = ConnectionId.random();
    private List<Version> supported = Lists.newArrayList(Version.DRAFT_15, Version.FINAL);
    private VersionNegotiationPacket packet = new VersionNegotiationPacket(of(dest), of(src), supported);

    @Test
    public void roundtrip() {
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);

        VersionNegotiationPacket parsed = VersionNegotiationPacket.parse(bb);

        assertEquals(dest, parsed.getDestinationConnectionId().get());
        assertEquals(src, parsed.getSourceConnectionId().get());
        assertEquals(supported, parsed.getSupportedVersions());
    }

    @Test
    public void randomMarker() {
        // marker must be random except for first bit
        ByteBuf bb = Unpooled.buffer();
        packet.write(bb);
        byte marker1 = bb.readByte();

        ByteBuf bb2 = Unpooled.buffer();
        packet.write(bb2);
        byte marker2 = bb2.readByte();

        assertNotEquals(marker1, marker2);
        assertTrue((0x80 & marker1) == 0x80);
        assertTrue((0x80 & marker2) == 0x80);

    }
}