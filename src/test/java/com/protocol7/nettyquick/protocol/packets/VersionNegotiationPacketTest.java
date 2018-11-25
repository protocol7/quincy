package com.protocol7.nettyquick.protocol.packets;

import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.NullAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;

public class VersionNegotiationPacketTest {

  private ConnectionId dest = ConnectionId.random();
  private ConnectionId src = ConnectionId.random();
  private List<Version> supported = Lists.newArrayList(Version.DRAFT_15, Version.FINAL);
  private VersionNegotiationPacket packet =
      new VersionNegotiationPacket(of(dest), of(src), supported);

  private final AEAD aead = NullAEAD.create(ConnectionId.random(), true);

  @Test
  public void roundtrip() {
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    VersionNegotiationPacket parsed = VersionNegotiationPacket.parse(bb);

    assertEquals(dest, parsed.getDestinationConnectionId().get());
    assertEquals(src, parsed.getSourceConnectionId().get());
    assertEquals(supported, parsed.getSupportedVersions());
  }

  @Test
  public void randomMarker() {
    // marker must be random except for first bit
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);
    byte marker1 = bb.readByte();

    ByteBuf bb2 = Unpooled.buffer();
    packet.write(bb2, aead);
    byte marker2 = bb2.readByte();

    assertTrue((0x80 & marker1) == 0x80);
    assertTrue((0x80 & marker2) == 0x80);
  }
}
