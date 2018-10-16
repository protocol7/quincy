package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class InitialPacketTest {

    private ConnectionId destConnId = ConnectionId.random();
    private ConnectionId srcConnId = ConnectionId.random();
    private byte[] token = Rnd.rndBytes(16);

    @Test
    public void roundtrip() {
        InitialPacket packet = InitialPacket.create(
                Optional.of(destConnId),
                Optional.of(srcConnId),
                Optional.of(token),
                Lists.newArrayList(PingFrame.INSTANCE));

        ByteBuf bb = Unpooled.buffer();

        packet.write(bb);

        InitialPacket parsed = InitialPacket.parse(bb);

        assertEquals(destConnId, parsed.getDestinationConnectionId().get());
        assertEquals(srcConnId, parsed.getSourceConnectionId().get());
        assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
        assertEquals(packet.getVersion(), parsed.getVersion());
        assertArrayEquals(token, parsed.getToken().get());
        assertEquals(1, parsed.getPayload().getLength());
        assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
    }

    @Test
    public void roundtripEmpty() {
        InitialPacket packet = InitialPacket.create(
                empty(),
                empty(),
                empty(),
                Lists.newArrayList(PingFrame.INSTANCE));

        ByteBuf bb = Unpooled.buffer();

        packet.write(bb);

        InitialPacket parsed = InitialPacket.parse(bb);

        assertFalse(parsed.getDestinationConnectionId().isPresent());
        assertFalse(parsed.getSourceConnectionId().isPresent());
        assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
        assertEquals(packet.getVersion(), parsed.getVersion());
        assertFalse(parsed.getToken().isPresent());
        assertEquals(1, parsed.getPayload().getLength());
        assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
    }
}