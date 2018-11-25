package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.EncryptionLevel;
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
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.protocol7.nettyquick.client.ClientStateMachine.ClientState.Closed;
import static com.protocol7.nettyquick.client.ClientStateMachine.ClientState.Closing;

public class ClientConnection implements Connection {

  private final Logger log = LoggerFactory.getLogger(ClientConnection.class);

  private ConnectionId destConnectionId;
  private int lastDestConnectionIdLength;
  private Optional<ConnectionId> srcConnectionId = Optional.empty();
  private final PacketSender packetSender;
  private final InetSocketAddress serverAddress;
  private final StreamListener streamListener;

  private final AtomicReference<Version> version = new AtomicReference<>(Version.CURRENT);
  private final AtomicReference<PacketNumber> sendPacketNumber = new AtomicReference<>(new PacketNumber(0)); // TODO fix
  private final PacketBuffer packetBuffer;
  private final ClientStateMachine stateMachine;

  private final Streams streams;

  private AEAD initialAead;
  private AEAD handshakeAead;
  private AEAD oneRttAead;

  public ClientConnection(final ConnectionId destConnectionId,
                          final PacketSender packetSender,
                          final InetSocketAddress serverAddress,
                          final StreamListener streamListener) {
    this.destConnectionId = destConnectionId;
    this.packetSender = packetSender;
    this.serverAddress = serverAddress;
    this.streamListener = streamListener;
    this.stateMachine = new ClientStateMachine(this);
    this.streams = new Streams(this);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered, this.streams);

    initAEAD();
  }

  private void initAEAD() {
    this.initialAead = NullAEAD.create(destConnectionId, true);
  }

  public Future<Void> handshake() {
    MDC.put("actor", "client");
    return stateMachine.handshake();
  }

  public Packet sendPacket(Packet p) {
    if (stateMachine.getState() == Closing || stateMachine.getState() == Closed) {
      throw new IllegalStateException("Connection not open");
    }
    packetBuffer.send(p);
    return p;
  }

  public FullPacket sendPacket(Frame... frames) {
    return (FullPacket) sendPacket(new ShortPacket(new ShortHeader(false,
                               getDestinationConnectionId(),
                               nextSendPacketNumber(),
                               new Payload(frames))));
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return srcConnectionId;
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return Optional.ofNullable(destConnectionId);
  }

  public void setSourceConnectionId(Optional<ConnectionId> srcConnId) {
    this.srcConnectionId = srcConnId;
  }

  public void setDestinationConnectionId(ConnectionId destConnId) {
    this.destConnectionId = destConnId;
    initAEAD();
  }

  public int getLastDestConnectionIdLength() {
    return lastDestConnectionIdLength;
  }

  private void sendPacketUnbuffered(Packet packet) {
    packetSender.send(packet, serverAddress, getAEAD(EncryptionLevel.forPacket(packet))).awaitUninterruptibly(); // TODO fix

    log.debug("Client sent {}", packet);
  }

  public void onPacket(Packet packet) {
    log.debug("Client got {}", packet);

    if (packet.getDestinationConnectionId().isPresent()) {
      lastDestConnectionIdLength = packet.getDestinationConnectionId().get().getLength();
    } else {
      lastDestConnectionIdLength = 0;
    }

    if (stateMachine.getState() != ClientStateMachine.ClientState.BeforeInitial) {
      packetBuffer.onPacket(packet);
    }
    stateMachine.handlePacket(packet);
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

  public Version getVersion() {
    return version.get();
  }

  public PacketNumber lastAckedPacketNumber() {
    return packetBuffer.getLargestAcked();
  }

  public PacketNumber nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }

  public void resetSendPacketNumber() {
    sendPacketNumber.set(new PacketNumber(0));
  }

  public Stream openStream() {
    return streams.openStream(true, true, streamListener);
  }

  public Stream getOrCreateStream(StreamId streamId) {
    return streams.getOrCreate(streamId, streamListener);
  }

  public Future<Void> close() {
    stateMachine.closeImmediate();

    return packetSender.destroy();
  }

  public Future<Void> closeByPeer() {
    return packetSender.destroy();
  }

  public ClientStateMachine.ClientState getState() {
    return stateMachine.getState();
  }
}
