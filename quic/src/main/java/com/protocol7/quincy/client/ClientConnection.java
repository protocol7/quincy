package com.protocol7.quincy.client;

import static com.protocol7.quincy.connection.State.Closed;
import static com.protocol7.quincy.protocol.packets.Packet.getEncryptionLevel;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.Pipeline;
import com.protocol7.quincy.connection.InternalConnection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.logging.LoggingHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.reliability.AckDelay;
import com.protocol7.quincy.reliability.PacketBufferManager;
import com.protocol7.quincy.streams.DefaultStreamManager;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.streams.StreamManager;
import com.protocol7.quincy.termination.TerminationManager;
import com.protocol7.quincy.tls.CertificateValidator;
import com.protocol7.quincy.tls.ClientTlsManager;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.utils.Ticker;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.MDC;

public class ClientConnection implements InternalConnection {

  private ConnectionId remoteConnectionId;
  private int lastDestConnectionIdLength;
  private final Optional<ConnectionId> localConnectionId = of(ConnectionId.random());
  private final PacketSender packetSender;

  private final Version version;
  private final AtomicReference<Long> sendPacketNumber = new AtomicReference<>(0L);
  private final PacketBufferManager packetBuffer;
  private final ClientStateMachine stateMachine;
  private Optional<byte[]> token = Optional.empty();

  private final StreamManager streamManager;
  private final ClientTlsManager tlsManager;
  private final Pipeline pipeline;
  private final InetSocketAddress peerAddress;
  private final Timer timer;

  public ClientConnection(
      final Configuration configuration,
      final ConnectionId initialRemoteConnectionId,
      final StreamListener streamListener,
      final PacketSender packetSender,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress,
      final CertificateValidator certificateValidator,
      final Timer timer) {
    this.version = configuration.getVersion();
    this.remoteConnectionId = initialRemoteConnectionId;
    this.packetSender = packetSender;
    this.peerAddress = peerAddress;
    this.streamManager = new DefaultStreamManager(this, streamListener);

    final Ticker ticker = Ticker.systemTicker();

    this.packetBuffer =
        new PacketBufferManager(
            new AckDelay(configuration.getAckDelayExponent(), ticker), this, timer, ticker);
    this.tlsManager =
        new ClientTlsManager(
            localConnectionId.get(), configuration.toTransportParameters(), certificateValidator);

    final LoggingHandler logger = new LoggingHandler(true);

    final TerminationManager terminationManager =
        new TerminationManager(this, timer, configuration.getIdleTimeout(), TimeUnit.SECONDS);

    this.pipeline =
        new Pipeline(
            List.of(
                logger,
                tlsManager,
                packetBuffer,
                streamManager,
                flowControlHandler,
                terminationManager),
            List.of(packetBuffer, logger));

    this.stateMachine = new ClientStateMachine(this);
    this.timer = timer;
  }

  private void resetTlsSession() {
    tlsManager.resetTlsSession(remoteConnectionId);
  }

  public void handshake(final Promise promise) {
    MDC.put("actor", "client");
    tlsManager.handshake(getState(), this, stateMachine::setState, promise);
  }

  public Packet sendPacket(final Packet p) {
    if (stateMachine.getState() == Closed) {
      throw new IllegalStateException("Connection not open");
    }

    final Packet newPacket = pipeline.send(this, p);

    // check again if any handler closed the connection
    if (stateMachine.getState() == Closed) {
      throw new IllegalStateException("Connection not open");
    }

    sendPacketUnbuffered(newPacket);
    return newPacket;
  }

  public FullPacket send(final Frame... frames) {
    final Packet packet;
    if (tlsManager.available(EncryptionLevel.OneRtt)) {
      packet = ShortPacket.create(false, getRemoteConnectionId(), nextSendPacketNumber(), frames);
    } else if (tlsManager.available(EncryptionLevel.Handshake)) {
      packet =
          HandshakePacket.create(
              of(remoteConnectionId), localConnectionId, nextSendPacketNumber(), version, frames);
    } else {
      packet =
          InitialPacket.create(
              of(remoteConnectionId),
              localConnectionId,
              nextSendPacketNumber(),
              version,
              token,
              frames);
    }

    return (FullPacket) sendPacket(packet);
  }

  @Override
  public Optional<ConnectionId> getLocalConnectionId() {
    return localConnectionId;
  }

  @Override
  public Optional<ConnectionId> getRemoteConnectionId() {
    return Optional.ofNullable(remoteConnectionId);
  }

  public void setRemoteConnectionId(final ConnectionId remoteConnectionId, final boolean retry) {
    this.remoteConnectionId = requireNonNull(remoteConnectionId);

    if (retry) {
      resetTlsSession();
    }
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  public void setToken(final byte[] token) {
    this.token = of(token);
  }

  public int getLastDestConnectionIdLength() {
    return lastDestConnectionIdLength;
  }

  private void sendPacketUnbuffered(final Packet packet) {
    packetSender
        .send(packet, getAEAD(getEncryptionLevel(packet)))
        .awaitUninterruptibly(); // TODO fix
  }

  public void onPacket(final Packet packet) {
    if (packet.getDestinationConnectionId().isPresent()) {
      lastDestConnectionIdLength = packet.getDestinationConnectionId().get().getLength();
    } else {
      lastDestConnectionIdLength = 0;
    }

    final EncryptionLevel encLevel = getEncryptionLevel(packet);
    if (tlsManager.available(encLevel)) {
      stateMachine.handlePacket(packet);
      if (getState() != State.Closed) {
        pipeline.onPacket(this, packet);
      }
    } else {
      // TODO handle unencryptable packet
    }
  }

  @Override
  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsManager.getAEAD(level);
  }

  public Version getVersion() {
    return version;
  }

  private long nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> PacketNumber.next(packetNumber));
  }

  public void resetSendPacketNumber() {
    sendPacketNumber.set(0L);
  }

  public Stream openStream() {
    return streamManager.openStream(true, true);
  }

  public Future<Void> close(
      final TransportError error, final FrameType frameType, final String msg) {
    stateMachine.closeImmediate(new ConnectionCloseFrame(error.getValue(), frameType, msg));

    return closeInternal();
  }

  @Override
  public InetSocketAddress getPeerAddress() {
    return peerAddress;
  }

  public Future<Void> close() {
    stateMachine.closeImmediate();

    return closeInternal();
  }

  public void closeByPeer() {
    closeInternal().awaitUninterruptibly(); // TODO fis
  }

  private Future<Void> closeInternal() {
    timer.stop();

    return packetSender.destroy();
  }

  public State getState() {
    return stateMachine.getState();
  }

  public void setState(final State state) {
    stateMachine.setState(state);
  }
}
