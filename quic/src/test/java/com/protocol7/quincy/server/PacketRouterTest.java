package com.protocol7.quincy.server;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PacketRouterTest {

  private AEAD aead = TestAEAD.create();

  private PacketRouter router;

  private ConnectionId destConnId = ConnectionId.random();
  private ConnectionId srcConnId = ConnectionId.random();
  @Mock private Connections connections;
  @Mock private ServerConnection connection;
  @Mock private StreamListener listener;
  @Mock private PacketSender sender;
  private InetSocketAddress peerAddress = TestUtil.getTestAddress();

  @Before
  public void setUp() {
    router = new PacketRouter(Version.DRAFT_18, connections, listener);

    when(connections.get(any(), any(), any(), any())).thenReturn(connection);

    when(connection.getAEAD(any())).thenReturn(aead);
    when(connection.getLocalConnectionId()).thenReturn(of(srcConnId));
  }

  @Test
  public void route() {
    final InitialPacket packet =
        InitialPacket.create(
            of(destConnId), empty(), 2, Version.DRAFT_18, empty(), new PaddingFrame(1));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    verify(connection).onPacket(packet);
  }

  @Test(expected = RuntimeException.class)
  public void invalidPacket() {
    final ByteBuf bb = Unpooled.wrappedBuffer("this is not a packet".getBytes());

    router.route(bb, sender, peerAddress);
  }

  @Test
  public void versionMismatch() {
    final InitialPacket packet =
        InitialPacket.create(
            of(destConnId), empty(), 2, Version.FINAL, empty(), new PaddingFrame(1));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    final ArgumentCaptor<VersionNegotiationPacket> captor =
        ArgumentCaptor.forClass(VersionNegotiationPacket.class);
    verify(sender).send(captor.capture(), any());

    final VersionNegotiationPacket verNeg = captor.getValue();

    assertEquals(destConnId, verNeg.getDestinationConnectionId().get());
    assertEquals(srcConnId, verNeg.getSourceConnectionId().get());
    assertEquals(List.of(Version.DRAFT_18), verNeg.getSupportedVersions());
  }
}
