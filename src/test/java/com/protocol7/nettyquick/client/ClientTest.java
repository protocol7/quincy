package com.protocol7.nettyquick.client;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.protocol.packets.*;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.tls.KeyUtil;
import com.protocol7.nettyquick.tls.ServerTlsSession;
import com.protocol7.nettyquick.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.util.concurrent.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
  @Mock private InetSocketAddress serverAddress;
  @Mock private StreamListener streamListener;

  @Before
  public void setUp() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, CertificateException {
    MockitoAnnotations.initMocks(this);

    when(packetSender.send(any(), any(), any())).thenReturn(new SucceededFuture(new DefaultEventExecutor(), null));

    connection = new ClientConnection(destConnectionId, packetSender, serverAddress, streamListener);

    PrivateKey privateKey = KeyUtil.getPrivateKeyFromPem("src/test/resources/server.key");
    byte[] serverCert = KeyUtil.getCertFromCrt("src/test/resources/server.crt").getEncoded();

    serverTlsSession = new ServerTlsSession(ImmutableList.of(serverCert), privateKey);
  }

  @Test
  public void handshake() {
    // start handshake
    Future<Void> handshakeFuture = connection.handshake();

    // validate first packet sent
    InitialPacket initialPacket = (InitialPacket) captureSentPacket(1);
    assertEquals(1, initialPacket.getPacketNumber().asLong());
    assertEquals(destConnectionId, initialPacket.getDestinationConnectionId().get());
    assertFalse(initialPacket.getSourceConnectionId().isPresent());
    assertFalse(initialPacket.getToken().isPresent());
    assertEquals(Version.CURRENT, initialPacket.getVersion());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);
    assertTrue(initialPacket.getPayload().getLength() >= 1200);

    // verify handshake state
    assertFalse(handshakeFuture.isDone());

    byte[] retryToken = Rnd.rndBytes(20);

    // first packet did not contain token, so server send retry
    connection.onPacket(new RetryPacket(
            Version.CURRENT,
            Optional.empty(),
            Optional.of(srcConnectionId),
            destConnectionId,
            retryToken));

    // validate new initial packet sent
    InitialPacket initialPacket2 = (InitialPacket) captureSentPacket(2);
    assertEquals(1, initialPacket2.getPacketNumber().asLong());
    ConnectionId newDestConnId = initialPacket2.getDestinationConnectionId().get();
    assertNotEquals(destConnectionId, newDestConnId);
    assertEquals(srcConnectionId, initialPacket2.getSourceConnectionId().get());
    assertArrayEquals(retryToken, initialPacket2.getToken().get());
    assertEquals(Version.CURRENT, initialPacket2.getVersion());

    CryptoFrame cf = (CryptoFrame) initialPacket2.getPayload().getFrames().get(0);

    byte[] clientHello = cf.getCryptoData();

    assertTrue(initialPacket2.getPayload().getLength() >= 1200);

    // verify handshake state
    assertFalse(handshakeFuture.isDone());

    ServerHelloAndHandshake shah = serverTlsSession.handleClientHello(clientHello);

    // receive server hello
    connection.onPacket(InitialPacket.create(
            Optional.of(newDestConnId),
            Optional.of(srcConnectionId),
            nextPacketNumber(),
            Version.CURRENT,
            Optional.empty(),
            new CryptoFrame(0, shah.getServerHello())));

    // verify no packet sent here
    verify(packetSender, times(2)).send(any(), any(), any());
    // verify handshake state
    assertFalse(handshakeFuture.isDone());

    // receive server handshake
    connection.onPacket(HandshakePacket.create(
            Optional.of(newDestConnId),
            Optional.of(srcConnectionId),
            nextPacketNumber(),
            Version.CURRENT,
            new CryptoFrame(0, shah.getServerHandshake())));

    // validate client fin handshake packet
    HandshakePacket hp = (HandshakePacket) captureSentPacket(3);
    assertEquals(2, hp.getPacketNumber().asLong());
    assertEquals(srcConnectionId, hp.getDestinationConnectionId().get()); // TODO quic-go requires this, but is it correct?
    assertEquals(srcConnectionId, hp.getSourceConnectionId().get());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);

    // verify that handshake is complete
    assertTrue(handshakeFuture.isDone());
  }

  @Test
  public void streamFrame() {
    handshake();

    connection.onPacket(packet(new StreamFrame(streamId, 0, true, DATA)));

    ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener).onData(any(), dataCaptor.capture());

    assertArrayEquals(DATA, dataCaptor.getValue());

    // verify ack
    assertAck(4, 3, 3, 3);
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
    assertAck(4, 3, 3, 3);
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
    assertAck(4, 3, 3, 3);
    assertAck(5, 4, 4, 4);
  }

  @Test
  public void resetStreamFrame() {
    handshake();

    connection.onPacket(packet(new RstStreamFrame(streamId, 123, 0)));

    verify(streamListener).onReset(any(Stream.class), eq(123), eq(0L));

    // verify ack
    assertAck(4, 3, 3, 3);
  }

  @Test
  public void ping() {
    handshake();

    connection.onPacket(packet(PingFrame.INSTANCE));

    // verify ack
    assertAck(4, 3, 3, 3);
  }

  private void assertAck(int number, int packetNumber, int smallest, int largest) {
    ShortPacket ackPacket = (ShortPacket) captureSentPacket(number);
    assertEquals(packetNumber, ackPacket.getPacketNumber().asLong());
    assertEquals(srcConnectionId, ackPacket.getDestinationConnectionId().get());
    assertEquals(new Payload(new AckFrame(123, new AckBlock(smallest, largest))), ackPacket.getPayload());
  }

  @Test
  public void frameBeforeHandshake() {
    // not handshaking

    connection.onPacket(packet(PingFrame.INSTANCE));

    // ignoring in unexpected state, nothing should happen
    verify(packetSender, never()).send(any(), any(), any());
  }

  private Packet captureSentPacket(int number) {
    ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(packetSender, atLeast(number)).send(packetCaptor.capture(), any(), any());

    List<Packet> values = packetCaptor.getAllValues();
    return values.get(number - 1);
  }

  private Packet packet(Frame... frames) {
    return new ShortPacket(new ShortHeader(false,
                           Optional.of(srcConnectionId), // TODO correct?
                           nextPacketNumber(),
                           new Payload(frames)));
  }

  private PacketNumber nextPacketNumber() {
    packetNumber = packetNumber.next();
    return packetNumber;
  }
}