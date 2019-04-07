package com.protocol7.nettyquic.client;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.protocol7.nettyquic.TestUtil;
import com.protocol7.nettyquic.connection.PacketSender;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.flowcontrol.FlowControlHandler;
import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.*;
import com.protocol7.nettyquic.protocol.packets.*;
import com.protocol7.nettyquic.streams.DefaultStream;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.nettyquic.tls.KeyUtil;
import com.protocol7.nettyquic.tls.ServerTlsSession;
import com.protocol7.nettyquic.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.nettyquic.tls.aead.InitialAEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import com.protocol7.nettyquic.utils.Rnd;
import io.netty.util.concurrent.*;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClientTest {

  private static final byte[] DATA = "Hello".getBytes();
  private static final byte[] DATA2 = "world".getBytes();

  private ClientConnection connection;
  private ServerTlsSession serverTlsSession;

  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  private PacketNumber packetNumber = new PacketNumber(0);
  private StreamId streamId = StreamId.random(true, true);

  @Mock private PacketSender packetSender;
  @Mock private StreamListener streamListener;
  @Mock private FlowControlHandler flowControlHandler;

  @Before
  public void setUp() {
    when(packetSender.send(any(), any()))
        .thenReturn(new SucceededFuture(new DefaultEventExecutor(), null));
    when(packetSender.destroy())
        .thenReturn(new DefaultPromise<Void>(GlobalEventExecutor.INSTANCE).setSuccess(null));

    connection =
        new ClientConnection(
            Version.DRAFT_18,
            destConnectionId,
            streamListener,
            packetSender,
            flowControlHandler,
            TestUtil.getTestAddress());

    PrivateKey privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");
    List<byte[]> serverCert = KeyUtil.getCertsFromCrt("src/test/resources/server.crt");

    serverTlsSession =
        new ServerTlsSession(
            InitialAEAD.create(Rnd.rndBytes(4), false),
            TransportParameters.defaults(Version.DRAFT_18.asBytes()),
            serverCert,
            privateKey);
  }

  @Test
  public void handshake() {
    // start handshake
    Future<Void> handshakeFuture = connection.handshake();

    // validate first packet sent
    InitialPacket initialPacket = (InitialPacket) captureSentPacket(1);
    assertEquals(1, initialPacket.getPacketNumber().asLong());
    assertEquals(destConnectionId, initialPacket.getDestinationConnectionId().get());
    assertTrue(initialPacket.getSourceConnectionId().isPresent());

    ConnectionId generatedSrcConnId = initialPacket.getSourceConnectionId().get();

    assertFalse(initialPacket.getToken().isPresent());
    assertEquals(Version.DRAFT_18, initialPacket.getVersion());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);
    assertTrue(initialPacket.getPayload().calculateLength() >= 1200);

    // verify handshake state
    assertFalse(handshakeFuture.isDone());
    assertEquals(State.BeforeHello, connection.getState());

    byte[] retryToken = Rnd.rndBytes(20);

    // first packet did not contain token, server sends retry
    connection.onPacket(
        new RetryPacket(
            Version.DRAFT_18,
            Optional.empty(),
            Optional.of(srcConnectionId),
            destConnectionId,
            retryToken));

    // validate new initial packet sent
    InitialPacket initialPacket2 = (InitialPacket) captureSentPacket(2);
    assertEquals(1, initialPacket2.getPacketNumber().asLong());
    ConnectionId newDestConnId = initialPacket2.getDestinationConnectionId().get();
    assertEquals(srcConnectionId, newDestConnId);
    assertEquals(generatedSrcConnId, initialPacket2.getSourceConnectionId().get());
    assertArrayEquals(retryToken, initialPacket2.getToken().get());
    assertEquals(Version.DRAFT_18, initialPacket2.getVersion());

    CryptoFrame cf = (CryptoFrame) initialPacket2.getPayload().getFrames().get(0);

    byte[] clientHello = cf.getCryptoData();

    assertTrue(initialPacket2.getPayload().calculateLength() >= 1200);

    // verify handshake state
    assertFalse(handshakeFuture.isDone());
    assertEquals(State.BeforeHello, connection.getState());

    ServerHelloAndHandshake shah = serverTlsSession.handleClientHello(clientHello);

    // receive server hello
    connection.onPacket(
        InitialPacket.create(
            Optional.of(newDestConnId),
            Optional.of(srcConnectionId),
            nextPacketNumber(),
            Version.DRAFT_18,
            Optional.empty(),
            new CryptoFrame(0, shah.getServerHello())));

    // verify no packet sent here
    verify(packetSender, times(2)).send(any(), any());

    // verify handshake state
    assertFalse(handshakeFuture.isDone());
    assertEquals(State.BeforeHandshake, connection.getState());

    // receive server handshake
    connection.onPacket(
        HandshakePacket.create(
            Optional.of(newDestConnId),
            Optional.of(srcConnectionId),
            nextPacketNumber(),
            Version.DRAFT_18,
            new CryptoFrame(0, shah.getServerHandshake())));

    // validate client fin handshake packet
    HandshakePacket hp = (HandshakePacket) captureSentPacket(3);
    assertEquals(2, hp.getPacketNumber().asLong());
    assertEquals(generatedSrcConnId, initialPacket2.getSourceConnectionId().get());
    assertEquals(srcConnectionId, hp.getDestinationConnectionId().get());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);

    // verify that handshake is complete
    assertTrue(handshakeFuture.isDone());
    assertEquals(State.Ready, connection.getState());
  }

  @Test
  public void streamFrame() {
    handshake();

    connection.onPacket(packet(new StreamFrame(streamId, 0, true, DATA)));

    ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener).onData(any(), dataCaptor.capture());

    assertArrayEquals(DATA, dataCaptor.getValue());

    // verify ack
    assertAck(4, 3, 2, 3);
  }

  @Test
  public void streamFrameInOrder() {
    handshake();

    connection.onPacket(packet(new StreamFrame(streamId, 0, false, DATA)));
    connection.onPacket(packet(new StreamFrame(streamId, DATA.length, true, DATA2)));

    ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener, times(2)).onData(any(), dataCaptor.capture());

    List<byte[]> datas = dataCaptor.getAllValues();

    assertEquals(2, datas.size());
    assertArrayEquals(DATA, datas.get(0));
    assertArrayEquals(DATA2, datas.get(1));

    // verify ack
    assertAck(4, 3, 2, 3);
    // verify ack
    assertAck(5, 4, 4, 4);
  }

  @Test
  public void streamFrameOutOfOrder() {
    handshake();

    connection.onPacket(packet(new StreamFrame(streamId, DATA.length, true, DATA2)));
    connection.onPacket(packet(new StreamFrame(streamId, 0, false, DATA)));

    ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener, times(2)).onData(any(), dataCaptor.capture());

    List<byte[]> datas = dataCaptor.getAllValues();

    assertEquals(2, datas.size());
    assertArrayEquals(DATA, datas.get(0));
    assertArrayEquals(DATA2, datas.get(1));

    // verify acks
    assertAck(4, 3, 2, 3);
    assertAck(5, 4, 4, 4);
  }

  @Test
  public void resetStreamFrame() {
    handshake();

    connection.onPacket(packet(new ResetStreamFrame(streamId, 123, 0)));

    verify(streamListener).onReset(any(DefaultStream.class), eq(123), eq(0L));

    // verify ack
    assertAck(4, 3, 2, 3);
  }

  @Test
  public void ping() {
    handshake();

    connection.onPacket(packet(PingFrame.INSTANCE));

    // verify ack
    assertAck(4, 3, 2, 3);
  }

  @Test
  public void peerCloseConnection() {
    handshake();

    connection.onPacket(packet(ConnectionCloseFrame.connection(123, 124, "Closed")));

    // verify ack
    // TODO must be acked
    // assertAck(4, 3, 2, 3);

    assertEquals(State.Closed, connection.getState());

    try {
      connection.send(PingFrame.INSTANCE);
      fail("Must throw IllegalStateException");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void immediateCloseConnection() {
    handshake();

    connection.close().awaitUninterruptibly();

    assertEquals(State.Closed, connection.getState());

    try {
      connection.send(PingFrame.INSTANCE);
      fail("Must throw IllegalStateException");
    } catch (IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void versionNegotiation() {
    // start handshake
    Future<Void> handshakeFuture = connection.handshake();

    // validate first packet sent
    InitialPacket initialPacket = (InitialPacket) captureSentPacket(1);

    // server does not support this version and sends a VerNeg

    VersionNegotiationPacket verNeg =
        new VersionNegotiationPacket(
            Optional.of(destConnectionId), Optional.of(srcConnectionId), Version.FINAL);

    connection.onPacket(verNeg);

    // should not have sent any more packets
    verify(packetSender, times(1)).send(any(), any());

    // should close connection
    verify(packetSender).destroy();
  }

  private void assertAck(int number, int packetNumber, int smallest, int largest) {
    ShortPacket ackPacket = (ShortPacket) captureSentPacket(number);
    assertEquals(packetNumber, ackPacket.getPacketNumber().asLong());
    assertEquals(srcConnectionId, ackPacket.getDestinationConnectionId().get());

    assertEquals(
        new Payload(new AckFrame(123, new AckBlock(smallest, largest))), ackPacket.getPayload());
  }

  @Test
  public void frameBeforeHandshake() {
    // not handshaking

    connection.onPacket(packet(PingFrame.INSTANCE));

    // ignoring in unexpected state, nothing should happen
    verify(packetSender, never()).send(any(), any());
  }

  private Packet captureSentPacket(int number) {
    ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(packetSender, atLeast(number)).send(packetCaptor.capture(), any());

    List<Packet> values = packetCaptor.getAllValues();
    return values.get(number - 1);
  }

  private Packet packet(Frame... frames) {
    return new ShortPacket(
        false,
        Optional.of(srcConnectionId), // TODO correct?
        nextPacketNumber(),
        new Payload(frames));
  }

  private PacketNumber nextPacketNumber() {
    packetNumber = packetNumber.next();
    return packetNumber;
  }
}
