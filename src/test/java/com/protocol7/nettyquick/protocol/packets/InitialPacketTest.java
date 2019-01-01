package com.protocol7.nettyquick.protocol.packets;

import static java.util.Optional.empty;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.TestAEAD;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import org.junit.Test;

public class InitialPacketTest {

  private ConnectionId destConnId = ConnectionId.random();
  private ConnectionId srcConnId = ConnectionId.random();
  private byte[] token = Rnd.rndBytes(16);

  private final AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    InitialPacket packet =
        InitialPacket.create(
            Optional.of(destConnId),
            Optional.of(srcConnId),
            new PacketNumber(123),
            Version.CURRENT,
            Optional.of(token),
            Lists.newArrayList(PingFrame.INSTANCE));

    ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    InitialPacket parsed = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertEquals(srcConnId, parsed.getSourceConnectionId().get());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertArrayEquals(token, parsed.getToken().get());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
  }

  @Test
  public void roundtripEmpty() {
    InitialPacket packet =
        InitialPacket.create(
            Optional.ofNullable(destConnId),
            empty(),
            new PacketNumber(123),
            Version.CURRENT,
            empty(),
            Lists.newArrayList(PingFrame.INSTANCE));

    ByteBuf bb = Unpooled.buffer();

    packet.write(bb, aead);

    InitialPacket parsed = InitialPacket.parse(bb).complete(l -> aead);

    assertEquals(destConnId, parsed.getDestinationConnectionId().get());
    assertFalse(parsed.getSourceConnectionId().isPresent());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getVersion(), parsed.getVersion());
    assertFalse(parsed.getToken().isPresent());
    assertEquals(1 + AEAD.OVERHEAD, parsed.getPayload().calculateLength());
    assertTrue(parsed.getPayload().getFrames().get(0) instanceof PingFrame);
  }
}
