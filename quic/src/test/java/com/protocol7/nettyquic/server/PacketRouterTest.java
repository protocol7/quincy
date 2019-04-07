package com.protocol7.nettyquic.server;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.nettyquic.TestUtil;
import com.protocol7.nettyquic.connection.PacketSender;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.protocol.frames.PingFrame;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.VersionNegotiationPacket;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.TestAEAD;
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
    InitialPacket packet =
        InitialPacket.create(
            of(destConnId),
            empty(),
            new PacketNumber(2),
            Version.DRAFT_18,
            empty(),
            PingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    verify(connection).onPacket(packet);
  }

  @Test(expected = RuntimeException.class)
  public void invalidPacket() {
    ByteBuf bb = Unpooled.wrappedBuffer("this is not a packet".getBytes());

    router.route(bb, sender, peerAddress);
  }

  @Test
  public void versionMismatch() {
    InitialPacket packet =
        InitialPacket.create(
            of(destConnId),
            empty(),
            new PacketNumber(2),
            Version.FINAL,
            empty(),
            PingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    ArgumentCaptor<VersionNegotiationPacket> captor =
        ArgumentCaptor.forClass(VersionNegotiationPacket.class);
    verify(sender).send(captor.capture(), any());

    VersionNegotiationPacket verNeg = captor.getValue();

    assertEquals(destConnId, verNeg.getDestinationConnectionId().get());
    assertEquals(srcConnId, verNeg.getSourceConnectionId().get());
    assertEquals(List.of(Version.DRAFT_18), verNeg.getSupportedVersions());
  }
}
