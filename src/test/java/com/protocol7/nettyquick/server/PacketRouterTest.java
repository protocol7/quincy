package com.protocol7.nettyquick.server;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.client.PacketSender;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.protocol.packets.VersionNegotiationPacket;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.TestAEAD;
import com.protocol7.nettyquick.utils.Debug;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PacketRouterTest {

  private AEAD aead = TestAEAD.create();

  private PacketRouter router;

  private ConnectionId destConnId = ConnectionId.random();
  private ConnectionId srcConnId = ConnectionId.random();
  @Mock private Connections connections;
  @Mock private ServerConnection connection;
  @Mock private StreamListener listener;
  @Mock private InetSocketAddress clientAddress;
  @Mock private PacketSender sender;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    router = new PacketRouter(Version.CURRENT, connections, listener);

    when(connections.get(any())).thenReturn(of(connection));
    when(connections.get(any(), any(), any(), any())).thenReturn(connection);

    when(connection.getAEAD(any())).thenReturn(aead);
    when(connection.getSourceConnectionId()).thenReturn(of(srcConnId));
  }

  @Test
  public void route() {
    InitialPacket packet =
        InitialPacket.create(
            of(destConnId),
            empty(),
            new PacketNumber(2),
            Version.CURRENT,
            empty(),
            PingFrame.INSTANCE);

    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, clientAddress, sender);

    verify(connection).onPacket(packet);
  }

  @Test(expected = RuntimeException.class)
  public void invalidPacket() {
    ByteBuf bb = Unpooled.wrappedBuffer("this is not a packet".getBytes());

    router.route(bb, clientAddress, sender);
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

    Debug.buffer(bb);

    router.route(bb, clientAddress, sender);

    ArgumentCaptor<VersionNegotiationPacket> captor =
        ArgumentCaptor.forClass(VersionNegotiationPacket.class);
    verify(sender).send(captor.capture(), any(), any());

    VersionNegotiationPacket verNeg = captor.getValue();

    assertEquals(destConnId, verNeg.getDestinationConnectionId().get());
    assertEquals(srcConnId, verNeg.getSourceConnectionId().get());
    assertEquals(ImmutableList.of(Version.CURRENT), verNeg.getSupportedVersions());
  }
}
