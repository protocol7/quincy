package com.protocol7.quincy.server;

import static java.util.Optional.empty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.Pipeline;
import com.protocol7.quincy.addressvalidation.RetryToken;
import com.protocol7.quincy.addressvalidation.ServerRetryHandler;
import com.protocol7.quincy.connection.InternalConnection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.flowcontrol.FlowControlHandler;
import com.protocol7.quincy.logging.LoggingHandler;
import com.protocol7.quincy.protocol.*;
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
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.ServerTLSManager;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.extensions.TransportParameters;
import com.protocol7.quincy.utils.Ticker;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ServerConnection implements InternalConnection {

  private Optional<ConnectionId> remoteConnectionId = Optional.empty();
  private final Optional<ConnectionId> localConnectionId;
  private final PacketSender packetSender;
  private final Version version;
  private final AtomicReference<PacketNumber> sendPacketNumber =
      new AtomicReference<>(PacketNumber.MIN);
  private final ServerStateMachine stateMachine;

  private final ServerTLSManager tlsManager;
  private final Pipeline pipeline;
  private final InetSocketAddress peerAddress;
  private final StreamManager streamManager;

  public ServerConnection(
      final Configuration configuration,
      final ConnectionId localConnectionId,
      final StreamListener streamListener,
      final PacketSender packetSender,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress,
      final Timer timer) {
    this.version = configuration.getVersion();
    this.packetSender = packetSender;
    this.peerAddress = peerAddress;
    final TransportParameters transportParameters = configuration.toTransportParameters();

    this.streamManager = new DefaultStreamManager(this, streamListener);

    final Ticker ticker = Ticker.systemTicker();

    final PacketBufferManager packetBuffer =
        new PacketBufferManager(
            new AckDelay(configuration.getAckDelayExponent(), ticker), this, timer, ticker);
    this.tlsManager =
        new ServerTLSManager(localConnectionId, transportParameters, privateKey, certificates);

    final LoggingHandler logger = new LoggingHandler(false);

    final TerminationManager terminationManager =
        new TerminationManager(this, timer, configuration.getIdleTimeout(), TimeUnit.SECONDS);

    this.pipeline =
        new Pipeline(
            List.of(
                logger,
                new ServerRetryHandler(new RetryToken(privateKey), 30, TimeUnit.MINUTES),
                tlsManager,
                packetBuffer,
                streamManager,
                flowControlHandler,
                terminationManager),
            List.of(flowControlHandler, packetBuffer, logger));

    this.localConnectionId = Optional.of(localConnectionId);

    this.stateMachine = new ServerStateMachine(this);
  }

  public Optional<ConnectionId> getRemoteConnectionId() {
    return remoteConnectionId;
  }

  public Optional<ConnectionId> getLocalConnectionId() {
    return localConnectionId;
  }

  public void setRemoteConnectionId(final ConnectionId remoteConnectionId) {
    this.remoteConnectionId = Optional.of(remoteConnectionId);
  }

  public Version getVersion() {
    return version;
  }

  public Packet sendPacket(final Packet p) {

    final Packet newPacket = pipeline.send(this, p);

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
              remoteConnectionId, localConnectionId, nextSendPacketNumber(), version, frames);
    } else {
      packet =
          InitialPacket.create(
              remoteConnectionId,
              localConnectionId,
              nextSendPacketNumber(),
              version,
              empty(),
              frames);
    }

    return (FullPacket) sendPacket(packet);
  }

  private void sendPacketUnbuffered(final Packet packet) {
    packetSender
        .send(packet, getAEAD(Packet.getEncryptionLevel(packet)))
        .awaitUninterruptibly(); // TODO fix
  }

  public void onPacket(final Packet packet) {
    // with incorrect conn ID
    stateMachine.processPacket(packet);

    pipeline.onPacket(this, packet);
  }

  @Override
  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsManager.getAEAD(level);
  }

  private PacketNumber nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }

  public State getState() {
    return stateMachine.getState();
  }

  public void setState(final State state) {
    stateMachine.setState(state);
  }

  public Future<Void> close(
      final TransportError error, final FrameType frameType, final String msg) {
    stateMachine.closeImmediate(new ConnectionCloseFrame(error.getValue(), frameType, msg));

    return packetSender.destroy();
  }

  public Stream openStream() {
    return streamManager.openStream(false, true);
  }

  @Override
  public InetSocketAddress getPeerAddress() {
    return peerAddress;
  }

  public Future<Void> close() {
    stateMachine.closeImmediate();

    return packetSender.destroy();
  }

  public void closeByPeer() {
    packetSender.destroy().awaitUninterruptibly(); // TOOD fix
  }
}
