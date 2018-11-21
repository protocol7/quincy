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

public class ClientStateMachineTest {

  private static final int HANDSHAKE_PACKETS = 3; // the number of packets sent during handshake
  private static final byte[] DATA = "Hello".getBytes();
  private static final byte[] DATA2 = "world".getBytes();

  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  @Mock private PacketSender packetSender;
  private ClientConnection connection;
  @Mock private InetSocketAddress serverAddress;
  @Mock private StreamListener streamListener;
  private ClientStateMachine stm;
  private PacketNumber packetNumber = new PacketNumber(0);
  private StreamId streamId = StreamId.random(true, true);
  private ServerTlsSession serverTlsSession;


  @Before
  public void setUp() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException, CertificateException {
    MockitoAnnotations.initMocks(this);

    when(packetSender.send(any(), any(), any())).thenReturn(new SucceededFuture(new DefaultEventExecutor(), null));

    connection = new ClientConnection(destConnectionId, packetSender, serverAddress, streamListener);

    stm = new ClientStateMachine(connection);

    PrivateKey privateKey = KeyUtil.getPrivateKeyFromPem("src/test/resources/server.key");
    byte[] serverCert = KeyUtil.getCertFromCrt("src/test/resources/server.crt").getEncoded();

    serverTlsSession = new ServerTlsSession(ImmutableList.of(serverCert), privateKey);

  }

  @Test
  public void handshake() {
    assertEquals(ClientStateMachine.ClientState.BeforeInitial, stm.getState());

    // start handshake
    Future<Void> handshakeFuture = stm.handshake();

    // validate first packet sent
    InitialPacket initialPacket = (InitialPacket) captureSentPacket();
    assertEquals(1, initialPacket.getPacketNumber().asLong());
    assertEquals(destConnectionId, initialPacket.getDestinationConnectionId().get());
    assertFalse(initialPacket.getSourceConnectionId().isPresent());
    assertFalse(initialPacket.getToken().isPresent());
    assertEquals(Version.CURRENT, initialPacket.getVersion());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);
    assertTrue(initialPacket.getPayload().getLength() >= 1200);

    // verify statemachine state
    assertFalse(handshakeFuture.isDone());
    assertEquals(ClientStateMachine.ClientState.WaitingForServerHello, stm.getState());

    byte[] retryToken = Rnd.rndBytes(20);

    // first packet did not contain token, so server send retry
    stm.handlePacket(new RetryPacket(
            Version.CURRENT,
            Optional.empty(),
            Optional.of(srcConnectionId),
            destConnectionId,
            retryToken));

    // validate new initial packet sent
    InitialPacket initialPacket2 = (InitialPacket) captureSentPacket();
    assertEquals(1, initialPacket2.getPacketNumber().asLong());
    ConnectionId newDestConnId = initialPacket2.getDestinationConnectionId().get();
    assertNotEquals(destConnectionId, newDestConnId);
    assertEquals(srcConnectionId, initialPacket2.getSourceConnectionId().get());
    assertArrayEquals(retryToken, initialPacket2.getToken().get());
    assertEquals(Version.CURRENT, initialPacket2.getVersion());

    CryptoFrame cf = (CryptoFrame) initialPacket2.getPayload().getFrames().get(0);

    byte[] clientHello = cf.getCryptoData();

    assertTrue(initialPacket2.getPayload().getLength() >= 1200);

    // verify statemachine state
    assertFalse(handshakeFuture.isDone());
    assertEquals(ClientStateMachine.ClientState.WaitingForServerHello, stm.getState());

    ServerHelloAndHandshake shah = serverTlsSession.handleClientHello(clientHello);

    // receive server hello
    stm.handlePacket(InitialPacket.create(
            Optional.of(newDestConnId),
            Optional.of(srcConnectionId),
            nextPacketNumber(),
            Version.CURRENT,
            Optional.empty(),
            new CryptoFrame(0, shah.getServerHello())));

    // verify no packet sent here
    verify(packetSender, times(2)).send(any(), any(), any());

    // receive server handshake
    stm.handlePacket(HandshakePacket.create(
            Optional.of(newDestConnId),
            Optional.of(srcConnectionId),
            nextPacketNumber(),
            Version.CURRENT,
            new CryptoFrame(0, shah.getServerHandshake())));

    // validate client fin handshake packet
    HandshakePacket hp = (HandshakePacket) captureSentPacket();
    assertEquals(2, hp.getPacketNumber().asLong());
    assertEquals(srcConnectionId, hp.getDestinationConnectionId().get()); // TODO quic-go requires this, but is it correct?
    assertEquals(srcConnectionId, hp.getSourceConnectionId().get());
    assertTrue(initialPacket.getPayload().getFrames().get(0) instanceof CryptoFrame);

    // verify that handshake is complete
    assertTrue(handshakeFuture.isDone());
    assertEquals(ClientStateMachine.ClientState.Ready, stm.getState());
  }

  @Test
  public void streamFrame() {
    handshake();

    stm.handlePacket(packet(new StreamFrame(streamId, 0, true, DATA)));

    ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener).onData(any(), dataCaptor.capture());

    assertArrayEquals(DATA, dataCaptor.getValue());

    verifyPacketsSentAfterHandshake(0);
  }

  @Test
  public void streamFrameInOrder() {
    handshake();

    stm.handlePacket(packet(new StreamFrame(streamId, 0, false, DATA)));
    stm.handlePacket(packet(new StreamFrame(streamId, DATA.length, true, DATA2)));

    ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener, times(2)).onData(any(), dataCaptor.capture());

    List<byte[]> datas = dataCaptor.getAllValues();

    assertEquals(2, datas.size());
    assertArrayEquals(DATA, datas.get(0));
    assertArrayEquals(DATA2, datas.get(1));
  }

  @Test
  public void streamFrameOutOfOrder() {
    handshake();

    stm.handlePacket(packet(new StreamFrame(streamId, DATA.length, true, DATA2)));
    stm.handlePacket(packet(new StreamFrame(streamId, 0, false, DATA)));

    ArgumentCaptor<byte[]> dataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(streamListener, times(2)).onData(any(), dataCaptor.capture());

    List<byte[]> datas = dataCaptor.getAllValues();

    assertEquals(2, datas.size());
    assertArrayEquals(DATA, datas.get(0));
    assertArrayEquals(DATA2, datas.get(1));
  }

  @Test
  public void resetStreamFrame() {
    handshake();

    stm.handlePacket(packet(new RstStreamFrame(streamId, 123, 0)));

    verify(streamListener).onReset(any(Stream.class), eq(123), eq(0L));
  }

  @Test
  public void ping() {
    handshake();

    stm.handlePacket(packet(PingFrame.INSTANCE));

    // nothing should happen
    verifyPacketsSentAfterHandshake(0);
  }

  @Test
  public void frameBeforeHandshake() {
    // not handshaking
    assertEquals(ClientStateMachine.ClientState.BeforeInitial, stm.getState());

    stm.handlePacket(packet(PingFrame.INSTANCE));

    // ignoring in unexpected state, nothing should happen
    verify(packetSender, never()).send(any(), any(), any());
    assertEquals(ClientStateMachine.ClientState.BeforeInitial, stm.getState());
  }


  private void verifyPacketsSentAfterHandshake(int number) {
    verify(packetSender, times(HANDSHAKE_PACKETS + number)).send(any(), any(), any());
  }

  private Packet captureSentPacket() {
    ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(packetSender, atLeastOnce()).send(packetCaptor.capture(), any(), any());

    List<Packet> values = packetCaptor.getAllValues();
    return values.get(values.size() - 1);
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