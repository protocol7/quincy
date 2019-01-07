package com.protocol7.nettyquic.protocol.packets;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.protocol.frames.PingFrame;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.TestAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.Test;

public class HandshakePacketTest {

  private ConnectionId destConnId = ConnectionId.random();
  private ConnectionId srcConnId = ConnectionId.random();

  private final AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    HandshakePacket packet =
        HandshakePacket.create(
            Optional.of(destConnId),
            Optional.of(srcConnId),
            PacketNumber.MIN,
            Version.CURRENT,
            PingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    HandshakePacket parsed = HandshakePacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertEquals(srcConnId, parsed.getSourceConnectionId().get());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
  }
}
