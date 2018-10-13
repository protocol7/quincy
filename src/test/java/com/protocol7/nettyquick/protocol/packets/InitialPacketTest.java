package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
                Collections.emptyList());

        ByteBuf bb = Unpooled.buffer();

        packet.write(bb);

        InitialPacket parsed = InitialPacket.parse(bb);

        assertEquals(destConnId, parsed.getDestinationConnectionId().get());
        assertEquals(srcConnId, parsed.getSourceConnectionId().get());
        assertArrayEquals(token, parsed.getToken().get());
        assertEquals(0, parsed.getPayload().getLength());
    }
}