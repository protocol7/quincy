package com.protocol7.quincy.verneg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class VersionNegotiationHandlerTest {

  private final Version supported = Version.DRAFT_29;
  private final VersionNegotiationHandler handler = new VersionNegotiationHandler(supported);
  private final EmbeddedChannel channel = new EmbeddedChannel(handler);
  private final ConnectionId dcid = ConnectionId.random();
  private final ConnectionId scid = ConnectionId.random();
  private final AEAD aead = TestAEAD.create();

  @Test
  public void testLongPacketSupported() {
    final ByteBuf bb = longPacket(supported);

    channel.writeInbound(bb);

    final ByteBuf inbound = channel.readInbound();
    assertSame(bb, inbound);

    assertNull(channel.readOutbound());
  }

  @Test
  public void testLongPacketNotSupported() {
    final ByteBuf bb = longPacket(Version.FINAL);

    channel.writeInbound(bb);

    assertNull(channel.readInbound());

    final ByteBuf out = channel.readOutbound();

    final VersionNegotiationPacket verneg = VersionNegotiationPacket.parse(out).complete(l -> aead);

    assertEquals(scid, verneg.getDestinationConnectionId());
    assertEquals(dcid, verneg.getSourceConnectionId());
    assertEquals(List.of(supported), verneg.getSupportedVersions());
  }

  @Test
  public void testShortPacket() {
    final ShortPacket packet = ShortPacket.create(false, dcid, 123, new PaddingFrame(10));
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    channel.writeInbound(bb);

    final ByteBuf inbound = channel.readInbound();
    assertSame(bb, inbound);

    assertNull(channel.readOutbound());
  }

  private ByteBuf longPacket(final Version version) {
    final InitialPacket packet =
        InitialPacket.create(dcid, scid, 0, version, Optional.empty(), new PaddingFrame(10));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);
    return bb;
  }
}
