package com.protocol7.quincy.connection;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.addressvalidation.InsecureQuicTokenHandler;
import com.protocol7.quincy.addressvalidation.QuicTokenHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.TestAEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PacketRouterTest {

  private final AEAD aead = TestAEAD.create();

  private PacketRouter router;

  private final ConnectionId destConnId = ConnectionId.random();
  private final ConnectionId originalDestConnId = ConnectionId.random();
  private final ConnectionId srcConnId = ConnectionId.random();
  @Mock private Connections connections;
  @Mock private Connection connection;
  @Mock private StreamHandler listener;
  @Mock private PacketSender sender;
  @Mock private PrivateKey privateKey;
  private final List<byte[]> certificates = Collections.emptyList();
  private final InetSocketAddress peerAddress = TestUtil.getTestAddress();

  private final QuicTokenHandler tokenHandler = InsecureQuicTokenHandler.INSTANCE;

  private byte[] token;

  @Before
  public void setUp() {
    token = tokenHandler.writeToken(originalDestConnId, peerAddress);

    router =
        new PacketRouter(
            Version.DRAFT_29,
            connections,
            listener,
            tokenHandler,
            of(certificates),
            of(privateKey));

    when(connections.create(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(connection);

    when(connection.getAEAD(any())).thenReturn(aead);
  }

  @Test
  public void routeInitialWithToken() {
    final InitialPacket packet =
        InitialPacket.create(
            destConnId, srcConnId, 2, Version.DRAFT_29, Optional.of(token), new PaddingFrame(6));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    verify(connection).onPacket(packet);
  }

  @Test
  public void routeInitialWithInvalidToken() {
    final byte[] invalidToken = Arrays.copyOf(token, token.length);
    invalidToken[0]++; // make token invalid

    final InitialPacket packet =
        InitialPacket.create(
            destConnId,
            srcConnId,
            2,
            Version.DRAFT_29,
            Optional.of(invalidToken),
            new PaddingFrame(6));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    verify(connection, never()).onPacket(packet);
    verify(sender, never()).send(any(), any());
  }

  @Test
  public void routeInitialWithoutToken() {
    final InitialPacket packet =
        InitialPacket.create(
            destConnId, srcConnId, 2, Version.DRAFT_29, Optional.empty(), new PaddingFrame(6));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    // must not route
    verify(connection, never()).onPacket(any());

    // must send retry
    final ArgumentCaptor<RetryPacket> captor = ArgumentCaptor.forClass(RetryPacket.class);
    verify(sender).send(captor.capture(), Mockito.eq(null));

    final RetryPacket retry = captor.getValue();

    assertEquals(srcConnId, retry.getDestinationConnectionId());
    assertNotEquals(destConnId, retry.getSourceConnectionId()); // must create new scid

    retry.verify(destConnId);
  }

  @Test
  public void routeNonInitialWithConnection() {
    when(connections.get(destConnId)).thenReturn(Optional.of(connection));

    final ShortPacket packet =
        ShortPacket.create(false, destConnId, srcConnId, 2, new PaddingFrame(6));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    verify(connection).onPacket(packet);
  }

  @Test
  public void routeNonInitialWithoutConnection() {
    when(connections.get(destConnId)).thenReturn(Optional.empty());

    final ShortPacket packet =
        ShortPacket.create(false, destConnId, srcConnId, 2, new PaddingFrame(6));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    // packet must be dropped
    verify(connection, never()).onPacket(packet);
    verify(sender, never()).send(any(), any());
  }

  @Test
  public void versionMismatch() {
    final InitialPacket packet =
        InitialPacket.create(destConnId, srcConnId, 2, Version.FINAL, empty(), new PaddingFrame(6));

    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    router.route(bb, sender, peerAddress);

    final ArgumentCaptor<VersionNegotiationPacket> captor =
        ArgumentCaptor.forClass(VersionNegotiationPacket.class);
    verify(sender).send(captor.capture(), Mockito.eq(null));

    final VersionNegotiationPacket verNeg = captor.getValue();

    assertEquals(srcConnId, verNeg.getDestinationConnectionId());
    assertEquals(destConnId, verNeg.getSourceConnectionId());
    assertEquals(List.of(Version.DRAFT_29), verNeg.getSupportedVersions());
  }
}
