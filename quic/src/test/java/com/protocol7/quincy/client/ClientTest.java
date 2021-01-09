package com.protocol7.quincy.client;

import static com.protocol7.quincy.protocol.ConnectionId.EMPTY;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.connection.ClientConnection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.flowcontrol.MockFlowControlHandler;
import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.protocol.frames.*;
import com.protocol7.quincy.protocol.packets.*;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.KeyUtil;
import com.protocol7.quincy.tls.NoopCertificateValidator;
import com.protocol7.quincy.tls.ServerTlsSession;
import com.protocol7.quincy.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.utils.Rnd;
import io.netty.util.Timer;
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
  private long packetNumber = 0;
  private final long streamId = StreamId.next(-1, true, true);

  private final FlowControlHandler flowControlHandler = new MockFlowControlHandler();

  @Mock private PacketSender packetSender;
  @Mock private StreamHandler streamListener;
  @Mock private Timer scheduler;

  @Before
  public void setUp() {
    when(packetSender.send(any(), any()))
        .thenReturn(new SucceededFuture(new DefaultEventExecutor(), null));
    when(packetSender.destroy())
        .thenReturn(new DefaultPromise<Void>(GlobalEventExecutor.INSTANCE).setSuccess(null));

    connection =
        new ClientConnection(
            new QuicBuilder().withApplicationProtocols("http/0.9").configuration(),
            destConnectionId,
            srcConnectionId,
            streamListener,
            packetSender,
            flowControlHandler,
            TestUtil.getTestAddress(),
            new NoopCertificateValidator(),
            scheduler);

    final PrivateKey privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");
    final List<byte[]> serverCert = KeyUtil.getCertsFromCrt("src/test/resources/server.crt");

    serverTlsSession =
        new ServerTlsSession(
            InitialAEAD.create(Rnd.rndBytes(4), false),
            List.of("http/0.9"),
            new QuicBuilder().configuration().toTransportParameters(),
            serverCert,
            privateKey);
  }

  @Test
  public void handshake() {
    // start handshake
    final DefaultPromise<Void> handshakeFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    connection.handshake(handshakeFuture);

    // validate first packet sent
    final InitialPacket initialPacket = (InitialPacket) captureSentPacket(1);
    assertEquals(0, initialPacket.getPacketNumber());
    assertEquals(destConnectionId, initialPacket.getDestinationConnectionId());

    final ConnectionId generatedSrcConnId = initialPacket.getSourceConnectionId();

    assertFalse(initialPacket.getToken().isPresent());
    assertEquals(Version.DRAFT_29, initialPacket.getVersion());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);
    assertTrue(initialPacket.getPayload().calculateLength() >= 1200);

    // verify handshake state
    assertFalse(handshakeFuture.isDone());
    assertEquals(State.BeforeHello, connection.getState());

    final byte[] retryToken = Rnd.rndBytes(20);

    // first packet did not contain token, server sends retry
    connection.onPacket(
        RetryPacket.createOutgoing(
            Version.DRAFT_29, EMPTY, srcConnectionId, destConnectionId, retryToken));

    // validate new initial packet sent
    final InitialPacket initialPacket2 = (InitialPacket) captureSentPacket(2);
    assertEquals(0, initialPacket2.getPacketNumber());
    final ConnectionId newDestConnId = initialPacket2.getDestinationConnectionId();
    assertEquals(srcConnectionId, newDestConnId);
    assertEquals(generatedSrcConnId, initialPacket2.getSourceConnectionId());
    assertArrayEquals(retryToken, initialPacket2.getToken().get());
    assertEquals(Version.DRAFT_29, initialPacket2.getVersion());

    final CryptoFrame cf = (CryptoFrame) initialPacket2.getPayload().getFrames().get(0);

    final byte[] clientHello = cf.getCryptoData();

    assertTrue(initialPacket2.getPayload().calculateLength() >= 1200);

    // verify handshake state
    assertFalse(handshakeFuture.isDone());
    assertEquals(State.BeforeHello, connection.getState());

    final ServerHelloAndHandshake shah = serverTlsSession.handleClientHello(clientHello);

    // receive server hello
    connection.onPacket(
        InitialPacket.create(
            newDestConnId,
            srcConnectionId,
            nextPacketNumber(),
            Version.DRAFT_29,
            Optional.empty(),
            new CryptoFrame(0, shah.getServerHello())));

    // verify no packet sent here
    verify(packetSender, times(3)).send(any(), any());

    // verify handshake state
    assertFalse(handshakeFuture.isDone());
    assertEquals(State.BeforeHandshake, connection.getState());

    // receive server handshake
    connection.onPacket(
        HandshakePacket.create(
            newDestConnId,
            srcConnectionId,
            nextPacketNumber(),
            Version.DRAFT_29,
            new CryptoFrame(0, shah.getServerHandshake())));

    // validate client fin handshake packet
    final HandshakePacket hp = (HandshakePacket) captureSentPacket(4);
    assertEquals(2, hp.getPacketNumber());
    assertEquals(generatedSrcConnId, initialPacket2.getSourceConnectionId());
    assertEquals(srcConnectionId, hp.getDestinationConnectionId());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);

    connection.onPacket(
        ShortPacket.create(
            false,
            srcConnectionId,
            destConnectionId,
            nextPacketNumber(),
            HandshakeDoneFrame.INSTANCE));

    // verify that handshake is complete
    assertTrue(handshakeFuture.isDone());
    assertEquals(State.Done, connection.getState());
  }

  @Test
  public void streamFrame() {
    handshake();

    connection.onPacket(packet(new StreamFrame(streamId, 0, true, DATA)));

    final ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener).onData(any(), dataCaptor.capture(), eq(true));

    assertArrayEquals(DATA, dataCaptor.getValue());

    // verify ack
    assertAck(6, 4, 3, 3);
  }

  @Test
  public void streamFrameInOrder() {
    handshake();

    connection.onPacket(packet(new StreamFrame(streamId, 0, false, DATA)));
    connection.onPacket(packet(new StreamFrame(streamId, DATA.length, true, DATA2)));

    verify(streamListener).onData(any(), eq(DATA), eq(false));
    verify(streamListener).onData(any(), eq(DATA2), eq(true));

    // verify ack
    assertAck(6, 4, 3, 3);
    // verify ack
    assertAck(7, 5, 4, 4);
  }

  @Test
  public void streamFrameOutOfOrder() {
    handshake();

    connection.onPacket(packet(new StreamFrame(streamId, DATA.length, true, DATA2)));
    connection.onPacket(packet(new StreamFrame(streamId, 0, false, DATA)));

    verify(streamListener).onData(any(), eq(DATA), eq(false));
    verify(streamListener).onData(any(), eq(DATA2), eq(true));

    // verify acks
    assertAck(6, 4, 3, 3);
    assertAck(7, 5, 4, 4);
  }

  @Test
  public void resetStreamFrame() {
    handshake();

    connection.onPacket(packet(new ResetStreamFrame(streamId, 123, 0)));

    // verify ack
    assertAck(6, 4, 3, 3);
  }

  @Test
  public void ping() {
    handshake();

    connection.onPacket(packet(PingFrame.INSTANCE));

    // verify ack
    assertAck(6, 4, 3, 3);
  }

  @Test
  public void peerCloseConnection() {
    handshake();

    connection.onPacket(packet(new ConnectionCloseFrame(123, FrameType.STREAM, "Closed")));

    // verify ack
    // TODO must be acked
    // assertAck(4, 3, 2, 3);

    assertEquals(State.Closed, connection.getState());

    try {
      connection.send(EncryptionLevel.OneRtt, PingFrame.INSTANCE);
      fail("Must throw IllegalStateException");
    } catch (final IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void immediateCloseConnection() {
    handshake();

    connection.close().awaitUninterruptibly();

    assertEquals(State.Closed, connection.getState());

    try {
      connection.send(EncryptionLevel.OneRtt, PingFrame.INSTANCE);
      fail("Must throw IllegalStateException");
    } catch (final IllegalStateException e) {
      // expected
    }
  }

  @Test
  public void versionNegotiation() {
    final DefaultPromise<Void> handshakeFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    // start handshake
    connection.handshake(handshakeFuture);

    // validate first packet sent
    final InitialPacket initialPacket = (InitialPacket) captureSentPacket(1);

    // server does not support this version and sends a VerNeg

    final VersionNegotiationPacket verNeg =
        new VersionNegotiationPacket(destConnectionId, srcConnectionId, Version.FINAL);

    connection.onPacket(verNeg);

    // should not have sent any more packets
    verify(packetSender, times(1)).send(any(), any());

    // should close connection
    verify(packetSender).destroy();
  }

  private void assertAck(
      final int number, final int packetNumber, final int smallest, final int largest) {
    final ShortPacket ackPacket = (ShortPacket) captureSentPacket(number);
    assertEquals(packetNumber, ackPacket.getPacketNumber());
    assertEquals(srcConnectionId, ackPacket.getDestinationConnectionId());

    final List<AckRange> actual =
        ((AckFrame) ackPacket.getPayload().getFrames().get(0)).getRanges();
    assertEquals(List.of(new AckRange(smallest, largest)), actual);
  }

  @Test(expected = IllegalStateException.class)
  public void frameBeforeHandshake() {
    // not handshaking

    connection.onPacket(packet(PingFrame.INSTANCE));
  }

  private Packet captureSentPacket(final int number) {
    final ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(packetSender, atLeast(number)).send(packetCaptor.capture(), any());

    final List<Packet> values = packetCaptor.getAllValues();
    return values.get(number - 1);
  }

  private Packet packet(final Frame... frames) {
    return ShortPacket.create(
        false,
        srcConnectionId, // TODO correct?
        destConnectionId,
        nextPacketNumber(),
        frames);
  }

  private long nextPacketNumber() {
    packetNumber = PacketNumber.next(packetNumber);
    return packetNumber;
  }
}
