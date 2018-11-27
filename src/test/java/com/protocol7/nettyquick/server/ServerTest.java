package com.protocol7.nettyquick.server;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.client.PacketSender;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.protocol.packets.*;
import com.protocol7.nettyquick.server.ServerStateMachine.ServerState;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.tls.ClientTlsSession;
import com.protocol7.nettyquick.tls.ClientTlsSession.HandshakeResult;
import com.protocol7.nettyquick.tls.KeyUtil;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServerTest {

  public static final byte[] DATA = "Hello".getBytes();
  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId destConnectionId2 = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  private ServerConnection connection;
  private PacketNumber packetNumber = new PacketNumber(0);
  private StreamId streamId = StreamId.random(true, true);

  private ClientTlsSession clientTlsSession = new ClientTlsSession();

  @Mock private PacketSender packetSender;
  @Mock private StreamListener streamListener;
  @Mock private InetSocketAddress clientAddress;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(packetSender.send(any(), any(), any()))
        .thenReturn(new SucceededFuture(new DefaultEventExecutor(), null));
    when(packetSender.destroy())
        .thenReturn(new DefaultPromise<Void>(GlobalEventExecutor.INSTANCE).setSuccess(null));

    List<byte[]> certificates = KeyUtil.getCertsFromCrt("src/test/resources/server.crt");
    PrivateKey privateKey = KeyUtil.getPrivateKeyFromPem("src/test/resources/server.key");

    connection =
        new ServerConnection(
            streamListener, packetSender, clientAddress, certificates, privateKey, srcConnectionId);
  }

  @Test
  public void handshake() {
    assertEquals(ServerState.BeforeInitial, connection.getState());
    byte[] ch = clientTlsSession.startHandshake();

    connection.onPacket(initialPacket(destConnectionId, empty(), new CryptoFrame(0, ch)));

    RetryPacket retry = (RetryPacket) captureSentPacket(1);
    assertFalse(retry.getDestinationConnectionId().isPresent());
    assertEquals(srcConnectionId, retry.getSourceConnectionId().get());
    assertEquals(destConnectionId, retry.getOriginalConnectionId());
    assertTrue(retry.getRetryToken().length > 0);
    byte[] token = retry.getRetryToken();

    connection.onPacket(initialPacket(destConnectionId2, of(token), new CryptoFrame(0, ch)));

    InitialPacket serverHello = (InitialPacket) captureSentPacket(2);
    assertEquals(destConnectionId2, serverHello.getDestinationConnectionId().get());
    assertEquals(srcConnectionId, serverHello.getSourceConnectionId().get());
    assertEquals(1, serverHello.getPacketNumber().asLong());
    assertFalse(serverHello.getToken().isPresent());
    assertEquals(1, serverHello.getPayload().getFrames().size());
    CryptoFrame cf = (CryptoFrame) serverHello.getPayload().getFrames().get(0);

    clientTlsSession.handleServerHello(cf.getCryptoData());

    HandshakePacket handshake = (HandshakePacket) captureSentPacket(3);
    assertEquals(destConnectionId2, handshake.getDestinationConnectionId().get());
    assertEquals(srcConnectionId, handshake.getSourceConnectionId().get());
    assertEquals(2, handshake.getPacketNumber().asLong());
    assertEquals(1, handshake.getPayload().getFrames().size());
    CryptoFrame cf2 = (CryptoFrame) handshake.getPayload().getFrames().get(0);

    HandshakeResult hr = clientTlsSession.handleHandshake(cf2.getCryptoData()).get();

    connection.onPacket(packet(destConnectionId2, new CryptoFrame(0, hr.getFin())));

    assertEquals(ServerState.Ready, connection.getState());
  }

  @Test
  public void streamFrame() {
    handshake();

    connection.onPacket(packet(destConnectionId2, new StreamFrame(streamId, 0, false, DATA)));

    verify(streamListener).onData(any(), eq(DATA));
  }

  @Test
  public void resetStreamFrame() {
    handshake();

    connection.onPacket(packet(destConnectionId2, new ResetStreamFrame(streamId, 123, 456)));

    verify(streamListener).onReset(any(), eq(123), eq(456L));
  }

  @Test
  public void ping() {
    handshake();

    connection.onPacket(packet(destConnectionId2, PingFrame.INSTANCE));

    assertAck(4, 3, 3, 3);
  }

  private void assertAck(int number, int packetNumber, int smallest, int largest) {
    ShortPacket ackPacket = (ShortPacket) captureSentPacket(number);
    assertEquals(packetNumber, ackPacket.getPacketNumber().asLong());
    assertEquals(destConnectionId2, ackPacket.getDestinationConnectionId().get());
    assertEquals(
        new Payload(new AckFrame(123, new AckBlock(smallest, largest))), ackPacket.getPayload());
  }

  @Test
  public void frameBeforeHandshake() {
    // not handshaking
    assertEquals(ServerState.BeforeInitial, connection.getState());

    connection.onPacket(packet(destConnectionId, PingFrame.INSTANCE));

    // ignoring in unexpected state, nothing should happen
    verify(packetSender, never()).send(any(), any(), any());
    assertEquals(ServerState.BeforeInitial, connection.getState());
  }

  private InitialPacket initialPacket(
      ConnectionId destConnId, Optional<byte[]> token, Frame... frames) {
    return InitialPacket.create(
        Optional.of(destConnId),
        Optional.of(srcConnectionId),
        nextPacketNumber(),
        Version.CURRENT,
        token,
        Lists.newArrayList(frames));
  }

  private Packet packet(ConnectionId destConnId, Frame... frames) {
    return new ShortPacket(
        new ShortHeader(false, Optional.of(destConnId), nextPacketNumber(), new Payload(frames)));
  }

  private PacketNumber nextPacketNumber() {
    packetNumber = packetNumber.next();
    return packetNumber;
  }

  private Packet captureSentPacket(int number) {
    ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(packetSender, atLeast(number)).send(packetCaptor.capture(), any(), any());

    List<Packet> values = packetCaptor.getAllValues();
    return values.get(number - 1);
  }
}
