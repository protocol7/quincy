package com.protocol7.nettyquic.server;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.protocol7.nettyquic.connection.PacketHandler;
import com.protocol7.nettyquic.connection.PacketSender;
import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.*;
import com.protocol7.nettyquic.protocol.packets.*;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.nettyquic.tls.ClientTlsSession;
import com.protocol7.nettyquic.tls.ClientTlsSession.HandshakeResult;
import com.protocol7.nettyquic.tls.KeyUtil;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;
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

  private ClientTlsSession clientTlsSession =
      new ClientTlsSession(TransportParameters.defaults(Version.DRAFT_18.asBytes()));

  @Mock private PacketSender packetSender;
  @Mock private StreamListener streamListener;
  @Mock private PacketHandler flowControlHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(packetSender.send(any(), any()))
        .thenReturn(new SucceededFuture(new DefaultEventExecutor(), null));
    when(packetSender.destroy())
        .thenReturn(new DefaultPromise<Void>(GlobalEventExecutor.INSTANCE).setSuccess(null));

    List<byte[]> certificates = KeyUtil.getCertsFromCrt("src/test/resources/server.crt");
    PrivateKey privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");

    connection =
        new ServerConnection(
            Version.DRAFT_18,
            srcConnectionId,
            streamListener,
            packetSender,
            certificates,
            privateKey,
            flowControlHandler);
  }

  @Test
  public void handshake() {
    assertEquals(ServerState.BeforeInitial, connection.getState());
    byte[] ch = clientTlsSession.startHandshake();

    connection.onPacket(initialPacket(destConnectionId, empty(), new CryptoFrame(0, ch)));

    RetryPacket retry = (RetryPacket) captureSentPacket(1);
    assertTrue(retry.getDestinationConnectionId().isPresent());
    assertEquals(srcConnectionId, retry.getDestinationConnectionId().get());
    assertEquals(destConnectionId, retry.getOriginalConnectionId());
    assertTrue(retry.getRetryToken().length > 0);
    byte[] token = retry.getRetryToken();

    connection.onPacket(initialPacket(destConnectionId2, of(token), new CryptoFrame(0, ch)));

    InitialPacket serverHello = (InitialPacket) captureSentPacket(2);
    assertEquals(srcConnectionId, serverHello.getDestinationConnectionId().get());

    assertTrue(serverHello.getSourceConnectionId().isPresent());

    ConnectionId newSourceConnectionId = serverHello.getSourceConnectionId().get();

    assertEquals(1, serverHello.getPacketNumber().asLong());
    assertFalse(serverHello.getToken().isPresent());
    assertEquals(1, serverHello.getPayload().getFrames().size());
    CryptoFrame cf = (CryptoFrame) serverHello.getPayload().getFrames().get(0);

    clientTlsSession.handleServerHello(cf.getCryptoData());

    HandshakePacket handshake = (HandshakePacket) captureSentPacket(3);
    assertEquals(srcConnectionId, handshake.getDestinationConnectionId().get());
    assertEquals(newSourceConnectionId, handshake.getSourceConnectionId().get());
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
    assertTrue(ackPacket.getDestinationConnectionId().isPresent());
    assertEquals(
        new Payload(new AckFrame(123, new AckBlock(smallest, largest))), ackPacket.getPayload());
  }

  @Test
  public void frameBeforeHandshake() {
    // not handshaking
    assertEquals(ServerState.BeforeInitial, connection.getState());

    connection.onPacket(packet(destConnectionId, PingFrame.INSTANCE));

    // ignoring in unexpected state, nothing should happen
    verify(packetSender, never()).send(any(), any());
    assertEquals(ServerState.BeforeInitial, connection.getState());
  }

  private InitialPacket initialPacket(
      ConnectionId destConnId, Optional<byte[]> token, Frame... frames) {
    return InitialPacket.create(
        Optional.of(destConnId),
        Optional.of(srcConnectionId),
        nextPacketNumber(),
        Version.DRAFT_18,
        token,
        List.of(frames));
  }

  private Packet packet(ConnectionId destConnId, Frame... frames) {
    return new ShortPacket(false, Optional.of(destConnId), nextPacketNumber(), new Payload(frames));
  }

  private PacketNumber nextPacketNumber() {
    packetNumber = packetNumber.next();
    return packetNumber;
  }

  private Packet captureSentPacket(int number) {
    ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(packetSender, atLeast(number)).send(packetCaptor.capture(), any());

    List<Packet> values = packetCaptor.getAllValues();
    return values.get(number - 1);
  }
}
