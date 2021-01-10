package com.protocol7.quincy.server;

import static java.util.Optional.empty;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.connection.AbstractConnection;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.protocol.frames.*;
import com.protocol7.quincy.protocol.packets.*;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.ClientTlsSession;
import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
import com.protocol7.quincy.tls.KeyUtil;
import com.protocol7.quincy.tls.NoopCertificateValidator;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import io.netty.buffer.Unpooled;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.SucceededFuture;
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
public class ServerTest {

  public static final byte[] DATA = "Hello".getBytes();
  private final ConnectionId serverConnectionId = ConnectionId.random();
  private final ConnectionId clientConnectionId = ConnectionId.random();
  private Connection connection;
  private long packetNumber = 0;
  private final long streamId = StreamId.next(-1, true, true);

  private final ClientTlsSession clientTlsSession =
      new ClientTlsSession(
          InitialAEAD.create(serverConnectionId.asBytes(), true),
          List.of("http/0.9"),
          new QuicBuilder().configuration().toTransportParameters(),
          NoopCertificateValidator.INSTANCE);

  @Mock private PacketSender packetSender;
  @Mock private StreamHandler streamListener;
  @Mock private Timer scheduler;
  private final FlowControlHandler flowControlHandler = new DefaultFlowControlHandler(1000, 1000);

  @Before
  public void setUp() {
    when(packetSender.send(any(), any()))
        .thenReturn(new SucceededFuture(new DefaultEventExecutor(), null));

    final List<byte[]> certificates = KeyUtil.getCertsFromCrt("src/test/resources/server.crt");
    final PrivateKey privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");

    connection =
        AbstractConnection.forServer(
            new QuicBuilder().withApplicationProtocols("http/0.9").configuration(),
            serverConnectionId,
            clientConnectionId,
            serverConnectionId,
            streamListener,
            packetSender,
            certificates,
            privateKey,
            flowControlHandler,
            TestUtil.getTestAddress(),
            scheduler);
  }

  @Test
  public void handshake() throws CertificateInvalidException {
    assertEquals(State.Started, connection.getState());
    final byte[] ch = clientTlsSession.startHandshake(serverConnectionId.asBytes());

    final InitialPacket clientHello =
        initialPacket(serverConnectionId, empty(), new CryptoFrame(0, ch));
    connection.onPacket(clientHello);

    final InitialPacket serverHello = (InitialPacket) captureSentPacket(1);
    assertEquals(clientConnectionId, serverHello.getDestinationConnectionId());

    final ConnectionId newSourceConnectionId = serverHello.getSourceConnectionId();

    assertEquals(0, serverHello.getPacketNumber());
    assertFalse(serverHello.getToken().isPresent());
    assertEquals(1, serverHello.getPayload().getFrames().size());
    final CryptoFrame cf = (CryptoFrame) serverHello.getPayload().getFrames().get(0);

    clientTlsSession.handleServerHello(Unpooled.wrappedBuffer(cf.getCryptoData()));

    final HandshakePacket handshake = (HandshakePacket) captureSentPacket(2);
    assertEquals(clientConnectionId, handshake.getDestinationConnectionId());
    assertEquals(newSourceConnectionId, handshake.getSourceConnectionId());
    assertEquals(1, handshake.getPacketNumber());
    assertEquals(1, handshake.getPayload().getFrames().size());
    final CryptoFrame cf2 = (CryptoFrame) handshake.getPayload().getFrames().get(0);

    final byte[] clientFin = clientTlsSession.handleHandshake(cf2.getCryptoData(), 0).get();

    connection.onPacket(hp(serverConnectionId, new CryptoFrame(0, clientFin)));

    final ShortPacket serverDone = (ShortPacket) captureSentPacket(4);

    connection.onPacket(
        ShortPacket.create(
            false,
            serverConnectionId,
            clientConnectionId,
            nextPacketNumber(),
            new AckFrame(
                123, new AckRange(serverDone.getPacketNumber(), serverDone.getPacketNumber()))));

    assertEquals(State.Done, connection.getState());
  }

  @Test
  public void streamFrame() throws CertificateInvalidException {
    handshake();

    connection.onPacket(packet(serverConnectionId, new StreamFrame(streamId, 0, false, DATA)));

    verify(streamListener).onData(any(), eq(DATA), eq(false));
  }

  @Test
  public void resetStreamFrame() throws CertificateInvalidException {
    handshake();

    connection.onPacket(packet(serverConnectionId, new ResetStreamFrame(streamId, 123, 456)));
  }

  @Test
  public void ping() throws CertificateInvalidException {
    handshake();

    connection.onPacket(packet(serverConnectionId, PingFrame.INSTANCE));

    assertAck(6, 5, 3, 4);
  }

  private void assertAck(
      final int number, final int packetNumber, final int smallest, final int largest) {
    final ShortPacket ackPacket = (ShortPacket) captureSentPacket(number);
    assertEquals(packetNumber, ackPacket.getPacketNumber());

    final List<AckRange> actual =
        ((AckFrame) ackPacket.getPayload().getFrames().get(0)).getRanges();
    assertEquals(List.of(new AckRange(smallest, largest)), actual);
  }

  @Test(expected = IllegalStateException.class)
  public void frameBeforeHandshake() {
    // not handshaking
    assertEquals(State.Started, connection.getState());

    connection.onPacket(packet(serverConnectionId, PingFrame.INSTANCE));
  }

  private InitialPacket initialPacket(
      final ConnectionId destConnId, final Optional<byte[]> token, final Frame... frames) {
    return InitialPacket.create(
        destConnId, clientConnectionId, nextPacketNumber(), Version.DRAFT_29, token, frames);
  }

  private Packet packet(final ConnectionId destConnId, final Frame... frames) {
    return ShortPacket.create(false, destConnId, clientConnectionId, nextPacketNumber(), frames);
  }

  private Packet hp(final ConnectionId destConnId, final Frame... frames) {
    return HandshakePacket.create(
        destConnId, clientConnectionId, nextPacketNumber(), Version.DRAFT_29, frames);
  }

  private long nextPacketNumber() {
    packetNumber = PacketNumber.next(packetNumber);
    return packetNumber;
  }

  private Packet captureSentPacket(final int number) {
    final ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(packetSender, atLeast(number)).send(packetCaptor.capture(), any());

    final List<Packet> values = packetCaptor.getAllValues();
    return values.get(number - 1);
  }
}
