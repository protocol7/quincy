package com.protocol7.nettyquic.client;

import static com.protocol7.nettyquic.client.ClientState.Closed;
import static com.protocol7.nettyquic.client.ClientState.Closing;
import static com.protocol7.nettyquic.protocol.packets.Packet.getEncryptionLevel;

import com.protocol7.nettyquic.Pipeline;
import com.protocol7.nettyquic.connection.Connection;
import com.protocol7.nettyquic.connection.PacketSender;
import com.protocol7.nettyquic.flowcontrol.FlowControlHandler;
import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.ConnectionCloseFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.FrameType;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.ShortPacket;
import com.protocol7.nettyquic.streams.DefaultStreamManager;
import com.protocol7.nettyquic.streams.Stream;
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.nettyquic.streams.StreamManager;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADs;
import com.protocol7.nettyquic.tls.aead.InitialAEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class ClientConnection implements Connection {

  private final Logger log = LoggerFactory.getLogger(ClientConnection.class);

  private ConnectionId remoteConnectionId;
  private int lastDestConnectionIdLength;
  private Optional<ConnectionId> localConnectionId = Optional.of(ConnectionId.random());
  private final PacketSender packetSender;

  private final Version version;
  private final AtomicReference<PacketNumber> sendPacketNumber =
      new AtomicReference<>(new PacketNumber(0));
  private final PacketBuffer packetBuffer;
  private final ClientStateMachine stateMachine;
  private Optional<byte[]> token = Optional.empty();

  private final StreamManager streamManager;
  private final Pipeline pipeline;
  private final InetSocketAddress peerAddress;

  private AEADs aeads;

  public ClientConnection(
      final Version version,
      final ConnectionId initialRemoteConnectionId,
      final StreamListener streamListener,
      final PacketSender packetSender,
      final FlowControlHandler flowControlHandler,
      final InetSocketAddress peerAddress) {
    this.version = version;
    this.remoteConnectionId = initialRemoteConnectionId;
    this.packetSender = packetSender;
    this.peerAddress = peerAddress;
    this.streamManager = new DefaultStreamManager(this, streamListener);

    this.pipeline = new Pipeline(List.of(streamManager, flowControlHandler), List.of());

    this.stateMachine =
        new ClientStateMachine(this, TransportParameters.defaults(version.asBytes()));
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered);

    initAEAD();
  }

  private void initAEAD() {
    this.aeads = new AEADs(InitialAEAD.create(remoteConnectionId.asBytes(), true));
  }

  public Future<Void> handshake() {
    MDC.put("actor", "client");
    return stateMachine.handshake();
  }

  public Packet sendPacket(final Packet p) {
    if (stateMachine.getState() == Closing || stateMachine.getState() == Closed) {
      throw new IllegalStateException("Connection not open");
    }

    pipeline.send(this, p);

    // TODO remove repeated check
    if (stateMachine.getState() == Closing || stateMachine.getState() == Closed) {
      throw new IllegalStateException("Connection not open");
    }

    packetBuffer.send(p);
    return p;
  }

  public FullPacket send(final Frame... frames) {
    return (FullPacket)
        sendPacket(
            new ShortPacket(
                false, getRemoteConnectionId(), nextSendPacketNumber(), new Payload(frames)));
  }

  @Override
  public Optional<ConnectionId> getLocalConnectionId() {
    return localConnectionId;
  }

  @Override
  public Optional<ConnectionId> getRemoteConnectionId() {
    return Optional.ofNullable(remoteConnectionId);
  }

  public void setRemoteConnectionId(final ConnectionId remoteConnectionId, boolean retry) {
    this.remoteConnectionId = remoteConnectionId;

    if (retry) {
      initAEAD();
    }
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  public void setToken(byte[] token) {
    this.token = Optional.of(token);
  }

  public int getLastDestConnectionIdLength() {
    return lastDestConnectionIdLength;
  }

  private void sendPacketUnbuffered(final Packet packet) {
    packetSender
        .send(packet, getAEAD(getEncryptionLevel(packet)))
        .awaitUninterruptibly(); // TODO fix

    log.debug("Client sent {}", packet);
  }

  public void onPacket(final Packet packet) {
    log.debug("Client got {}", packet);

    if (packet.getDestinationConnectionId().isPresent()) {
      lastDestConnectionIdLength = packet.getDestinationConnectionId().get().getLength();
    } else {
      lastDestConnectionIdLength = 0;
    }

    EncryptionLevel encLevel = getEncryptionLevel(packet);
    if (aeads.available(encLevel)) {
      packetBuffer.onPacket(packet);

      stateMachine.handlePacket(packet);

      pipeline.onPacket(this, packet);
    } else {
      // TODO handle unencryptable packet
    }
  }

  @Override
  public AEAD getAEAD(final EncryptionLevel level) {
    return aeads.get(level);
  }

  public void setHandshakeAead(final AEAD handshakeAead) {
    aeads.setHandshakeAead(handshakeAead);
  }

  public void setOneRttAead(final AEAD oneRttAead) {
    aeads.setOneRttAead(oneRttAead);
  }

  public Version getVersion() {
    return version;
  }

  public PacketNumber nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }

  public void resetSendPacketNumber() {
    sendPacketNumber.set(new PacketNumber(0));
  }

  public Stream openStream() {
    return streamManager.openStream(true, true);
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

  public ClientState getState() {
    return stateMachine.getState();
  }
}
