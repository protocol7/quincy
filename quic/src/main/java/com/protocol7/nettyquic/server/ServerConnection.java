package com.protocol7.nettyquic.server;

import static com.protocol7.nettyquic.tls.EncryptionLevel.Initial;
import static java.util.Optional.empty;

import com.protocol7.nettyquic.Configuration;
import com.protocol7.nettyquic.Pipeline;
import com.protocol7.nettyquic.addressvalidation.RetryToken;
import com.protocol7.nettyquic.addressvalidation.ServerRetryHandler;
import com.protocol7.nettyquic.connection.InternalConnection;
import com.protocol7.nettyquic.connection.PacketSender;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.flowcontrol.FlowControlHandler;
import com.protocol7.nettyquic.logging.LoggingHandler;
import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.ConnectionCloseFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.FrameType;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.HandshakePacket;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.ShortPacket;
import com.protocol7.nettyquic.reliability.PacketBuffer;
import com.protocol7.nettyquic.streams.DefaultStreamManager;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.nettyquic.streams.StreamManager;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.ServerTLSManager;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import com.protocol7.nettyquic.utils.Ticker;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ServerConnection implements InternalConnection {

  private Optional<ConnectionId> remoteConnectionId = Optional.empty();
  private Optional<ConnectionId> localConnectionId;
  private final PacketSender packetSender;
  private final Version version;
  private final AtomicReference<PacketNumber> sendPacketNumber =
      new AtomicReference<>(PacketNumber.MIN);
  private final ServerStateMachine stateMachine;

  private final ServerTLSManager tlsManager;
  private final Pipeline pipeline;
  private final InetSocketAddress peerAddress;

  public ServerConnection(
      final Configuration configuration,
      final ConnectionId localConnectionId,
      final StreamListener streamListener,
      final PacketSender packetSender,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress) {
    this.version = configuration.getVersion();
    this.packetSender = packetSender;
    this.peerAddress = peerAddress;
    final TransportParameters transportParameters = configuration.toTransportParameters();

    final StreamManager streamManager = new DefaultStreamManager(this, streamListener);
    final PacketBuffer packetBuffer =
        new PacketBuffer(configuration.getAckDelayExponent(), Ticker.systemTicker());
    this.tlsManager =
        new ServerTLSManager(localConnectionId, transportParameters, privateKey, certificates);

    final LoggingHandler logger = new LoggingHandler(false);

    this.pipeline =
        new Pipeline(
            List.of(
                logger,
                new ServerRetryHandler(new RetryToken(privateKey), 30, TimeUnit.MINUTES),
                tlsManager,
                packetBuffer,
                streamManager,
                flowControlHandler),
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

  public void setRemoteConnectionId(ConnectionId remoteConnectionId) {
    this.remoteConnectionId = Optional.of(remoteConnectionId);
  }

  public Version getVersion() {
    return version;
  }

  public Packet sendPacket(Packet p) {

    Packet newPacket = pipeline.send(this, p);

    sendPacketUnbuffered(newPacket);

    return newPacket;
  }

  public FullPacket send(final Frame... frames) {
    Packet packet;
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

  private void sendPacketUnbuffered(Packet packet) {
    packetSender.send(packet, getAEAD(Initial)).awaitUninterruptibly(); // TODO fix
  }

  public void onPacket(Packet packet) {
    // with incorrect conn ID
    stateMachine.processPacket(packet);

    pipeline.onPacket(this, packet);
  }

  @Override
  public AEAD getAEAD(EncryptionLevel level) {
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
    stateMachine.closeImmediate(
        ConnectionCloseFrame.connection(error.getValue(), frameType.getType(), msg));

    return packetSender.destroy();
  }

  @Override
  public InetSocketAddress getPeerAddress() {
    return peerAddress;
  }

  public Future<Void> close() {
    stateMachine.closeImmediate();

    return packetSender.destroy();
  }

  public Future<Void> closeByPeer() {
    return packetSender.destroy();
  }
}
