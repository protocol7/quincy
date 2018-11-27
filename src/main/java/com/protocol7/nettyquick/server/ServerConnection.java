package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.EncryptionLevel;
import com.protocol7.nettyquick.client.PacketSender;
import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.streams.Streams;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.NullAEAD;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerConnection implements Connection {

  private final Logger log = LoggerFactory.getLogger(ServerConnection.class);

  private Optional<ConnectionId> destConnectionId = Optional.empty();
  private final Optional<ConnectionId> srcConnectionId;
  private final StreamListener handler;
  private final PacketSender packetSender;
  private final InetSocketAddress clientAddress;
  private final AtomicReference<Version> version = new AtomicReference<>(Version.CURRENT);
  private final AtomicReference<PacketNumber> sendPacketNumber =
      new AtomicReference<>(PacketNumber.MIN);
  private final Streams streams;
  private final ServerStateMachine stateMachine;
  private final PacketBuffer packetBuffer;

  private AEAD initialAead;
  private AEAD handshakeAead;
  private AEAD oneRttAead;

  public ServerConnection(
      final StreamListener handler,
      final PacketSender packetSender,
      final InetSocketAddress clientAddress,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final ConnectionId srcConnId) {
    this.handler = handler;
    this.packetSender = packetSender;
    this.clientAddress = clientAddress;
    this.stateMachine = new ServerStateMachine(this, certificates, privateKey);
    this.streams = new Streams(this);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered, this.streams);

    this.srcConnectionId = Optional.of(srcConnId);

    this.initialAead = NullAEAD.create(srcConnId, true);
  }

  public Optional<ConnectionId> getDestinationConnectionId() {
    return destConnectionId;
  }

  public Optional<ConnectionId> getSourceConnectionId() {
    return srcConnectionId;
  }

  public void setDestinationConnectionId(ConnectionId destConnectionId) {
    this.destConnectionId = Optional.of(destConnectionId);
  }

  public Version getVersion() {
    return version.get();
  }

  public Packet sendPacket(Packet p) {
    packetBuffer.send(p);
    return p;
  }

  public FullPacket sendPacket(Frame... frames) {
    return (FullPacket)
        sendPacket(
            new ShortPacket(
                new ShortHeader(
                    false,
                    getDestinationConnectionId(),
                    nextSendPacketNumber(),
                    new Payload(frames))));
  }

  private void sendPacketUnbuffered(Packet packet) {
    packetSender.send(packet, clientAddress, initialAead).awaitUninterruptibly(); // TODO fix
    log.debug("Server sent {}", packet);
  }

  public void onPacket(Packet packet) {
    log.debug("Server got {}", packet);

    if (stateMachine.getState() != ServerStateMachine.ServerState.BeforeInitial) {
      packetBuffer.onPacket(packet);
    }
    // with incorrect conn ID
    stateMachine.processPacket(packet);
  }

  @Override
  public AEAD getAEAD(EncryptionLevel level) {
    if (level == EncryptionLevel.Initial) {
      log.debug("Using initial AEAD: {}", initialAead);
      return initialAead;
    } else if (level == EncryptionLevel.Handshake) {
      log.debug("Using handshake AEAD: {}", handshakeAead);
      return handshakeAead;
    } else {
      log.debug("Using 1-RTT AEAD: {}", oneRttAead);
      return oneRttAead;
    }
  }

  public void setHandshakeAead(AEAD handshakeAead) {
    this.handshakeAead = handshakeAead;
  }

  public void setOneRttAead(AEAD oneRttAead) {
    this.oneRttAead = oneRttAead;
  }

  public Stream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, handler);
  }

  public PacketNumber lastAckedPacketNumber() {
    return packetBuffer.getLargestAcked();
  }

  public PacketNumber nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }

  public ServerStateMachine.ServerState getState() {
    return stateMachine.getState();
  }
}
