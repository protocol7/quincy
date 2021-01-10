package com.protocol7.quincy.connection;

import static com.protocol7.quincy.connection.State.Closed;
import static java.util.Optional.of;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.Pipeline;
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
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.streams.StreamManager;
import com.protocol7.quincy.termination.TerminationManager;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.ServerTlsManager;
import com.protocol7.quincy.tls.TlsManager;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.utils.Ticker;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AbstractConnection implements Connection {

  public static AbstractConnection forServer(
      final Configuration configuration,
      final ConnectionId localConnectionId,
      final ConnectionId remoteConnectionId,
      final ConnectionId originalRemoteConnectionId,
      final StreamHandler streamListener,
      final PacketSender packetSender,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress,
      final Timer timer) {
    return new AbstractConnection(
        configuration.getVersion(),
        peerAddress,
        localConnectionId,
        remoteConnectionId,
        new ServerTlsManager(
            localConnectionId,
            originalRemoteConnectionId,
            configuration.getApplicationProtocols(),
            configuration.toTransportParameters(),
            privateKey,
            certificates),
        new ServerStateMachine(),
        packetSender,
        streamListener,
        false,
        flowControlHandler,
        configuration,
        timer);
  }

  private final Version version;

  private final InetSocketAddress peerAddress;

  private ConnectionId remoteConnectionId;
  private final ConnectionId localConnectionId;

  protected final TlsManager tlsManager;
  protected final StateMachine stateMachine;
  private final PacketSender packetSender;
  private final StreamManager streamManager;

  private final Pipeline pipeline;

  private final boolean isClient;

  private Optional<byte[]> token = Optional.empty();

  private final AtomicReference<Long> sendPacketNumber = new AtomicReference<>(-1L);

  protected AbstractConnection(
      final Version version,
      final InetSocketAddress peerAddress,
      final ConnectionId localConnectionId,
      final ConnectionId remoteConnectionId,
      final TlsManager tlsManager,
      final StateMachine stateMachine,
      final PacketSender packetSender,
      final StreamHandler streamHandler,
      final boolean isClient,
      final InboundHandler flowControlHandler,
      final Configuration configuration,
      final Timer timer) {
    this.version = version;
    this.peerAddress = peerAddress;
    this.localConnectionId = localConnectionId;
    this.remoteConnectionId = remoteConnectionId;
    this.tlsManager = tlsManager;
    this.stateMachine = stateMachine;
    this.packetSender = packetSender;
    this.streamManager = new DefaultStreamManager(this, streamHandler);
    this.isClient = isClient;

    final Ticker ticker = Ticker.systemTicker();

    final PacketBufferManager packetBuffer =
        new PacketBufferManager(
            new AckDelay(configuration.getAckDelayExponent(), ticker), this, timer, ticker);

    final LoggingHandler logger = new LoggingHandler(isClient);

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
  }

  public void onPacket(final Packet packet) {
    stateMachine.handlePacket(this, packet);
    pipeline.onPacket(this, packet);
  }

  @Override
  public boolean isOpen() {
    return getState() != Closed;
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

  private void sendPacketUnbuffered(final Packet packet) {
    packetSender
        .send(packet, getAEAD(Packet.getEncryptionLevel(packet)))
        .awaitUninterruptibly(); // TODO fix
  }

  public FullPacket send(final EncryptionLevel level, final Frame... frames) {
    final Packet packet;
    if (level == EncryptionLevel.OneRtt) {
      packet =
          ShortPacket.create(
              false, remoteConnectionId, localConnectionId, nextSendPacketNumber(), frames);
    } else if (level == EncryptionLevel.Handshake) {
      packet =
          HandshakePacket.create(
              remoteConnectionId, localConnectionId, nextSendPacketNumber(), version, frames);
    } else {
      packet =
          InitialPacket.create(
              remoteConnectionId,
              localConnectionId,
              nextSendPacketNumber(),
              version,
              token,
              frames);
    }

    return (FullPacket) sendPacket(packet);
  }

  public Version getVersion() {
    return version;
  }

  @Override
  public InetSocketAddress getPeerAddress() {
    return peerAddress;
  }

  public void setRemoteConnectionId(final ConnectionId remoteConnectionId) {
    this.remoteConnectionId = remoteConnectionId;
  }

  public State getState() {
    return stateMachine.getState();
  }

  public void setState(final State state) {
    stateMachine.setState(state);
  }

  @Override
  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsManager.getAEAD(level);
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  public void setToken(final byte[] token) {
    this.token = of(token);
  }

  private final List<Listener> closeListeners = new ArrayList<>();

  @Override
  public void addCloseListener(final Listener listener) {
    closeListeners.add(listener);
  }

  private long nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(PacketNumber::next);
  }

  private void resetSendPacketNumber() {
    sendPacketNumber.set(-1L);
  }

  public Stream openStream() {
    return streamManager.openStream(isClient, true);
  }

  public void reset(final ConnectionId remoteConnectionId) {
    setRemoteConnectionId(remoteConnectionId);

    resetSendPacketNumber();

    tlsManager.resetTlsSession(remoteConnectionId);
  }

  public Future<Void> close(
      final TransportError error, final FrameType frameType, final String msg) {
    stateMachine.closeImmediate(this, new ConnectionCloseFrame(error.getValue(), frameType, msg));

    notifyCloseListeners();

    return packetSender.destroy();
  }

  public Future<Void> close() {
    stateMachine.closeImmediate(this);

    notifyCloseListeners();

    return packetSender.destroy();
  }

  public void closeByPeer() {
    notifyCloseListeners();

    packetSender.destroy().awaitUninterruptibly(); // TOOD fix
  }

  private void notifyCloseListeners() {
    for (final Listener listener : closeListeners) {
      listener.action();
    }
  }
}
