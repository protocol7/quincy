package com.protocol7.nettyquic.server;

import static com.protocol7.nettyquic.tls.EncryptionLevel.Initial;

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
import com.protocol7.nettyquic.streams.StreamListener;
import com.protocol7.nettyquic.streams.StreamManager;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADs;
import com.protocol7.nettyquic.tls.aead.InitialAEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import io.netty.util.concurrent.Future;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerConnection implements Connection {

  private final Logger log = LoggerFactory.getLogger(ServerConnection.class);

  private Optional<ConnectionId> remoteConnectionId = Optional.empty();
  private Optional<ConnectionId> localConnectionId;
  private final PacketSender packetSender;
  private final Version version;
  private final AtomicReference<PacketNumber> sendPacketNumber =
      new AtomicReference<>(PacketNumber.MIN);
  private final ServerStateMachine stateMachine;
  private final PacketBuffer packetBuffer;

  private final StreamManager streamManager;
  private final Pipeline pipeline;

  private final TransportParameters transportParameters;

  private AEADs aeads;

  public ServerConnection(
      final Version version,
      final ConnectionId localConnectionId,
      final StreamListener streamListener,
      final PacketSender packetSender,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final FlowControlHandler flowControlHandler) {
    this.version = version;
    this.packetSender = packetSender;
    this.transportParameters = TransportParameters.defaults(version.asBytes());

    this.streamManager = new DefaultStreamManager(this, streamListener);

    this.pipeline =
        new Pipeline(
            List.of(new RetryHandler(), streamManager, flowControlHandler),
            List.of(flowControlHandler));

    this.stateMachine = new ServerStateMachine(this, transportParameters, privateKey, certificates);
    this.packetBuffer = new PacketBuffer(this, this::sendPacketUnbuffered);

    this.localConnectionId = Optional.of(localConnectionId);

    initAEAD();
  }

  private void initAEAD() {
    this.aeads = new AEADs(InitialAEAD.create(localConnectionId.get().asBytes(), false));
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

  public void setLocalConnectionId(ConnectionId localConnectionId) {
    this.localConnectionId = Optional.of(localConnectionId);

    initAEAD();
  }

  public Version getVersion() {
    return version;
  }

  public Packet sendPacket(Packet p) {

    pipeline.send(this, p);

    packetBuffer.send(p);
    return p;
  }

  public FullPacket send(Frame... frames) {
    return (FullPacket)
        sendPacket(
            new ShortPacket(
                false, getRemoteConnectionId(), nextSendPacketNumber(), new Payload(frames)));
  }

  private void sendPacketUnbuffered(Packet packet) {
    packetSender.send(packet, getAEAD(Initial)).awaitUninterruptibly(); // TODO fix
    log.debug("Server sent {}", packet);
  }

  public void onPacket(Packet packet) {
    log.debug("Server got {}", packet);

    if (stateMachine.getState() != ServerState.BeforeInitial) {
      packetBuffer.onPacket(packet);
    }
    // with incorrect conn ID
    stateMachine.processPacket(packet);

    pipeline.onPacket(this, packet);
  }

  @Override
  public AEAD getAEAD(EncryptionLevel level) {
    return aeads.get(level);
  }

  public void setHandshakeAead(AEAD handshakeAead) {
    aeads.setHandshakeAead(handshakeAead);
  }

  public void setOneRttAead(AEAD oneRttAead) {
    aeads.setOneRttAead(oneRttAead);
  }

  public PacketNumber nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> packetNumber.next());
  }

  public ServerState getState() {
    return stateMachine.getState();
  }

  public Future<Void> close(
      final TransportError error, final FrameType frameType, final String msg) {
    stateMachine.closeImmediate(
        ConnectionCloseFrame.connection(error.getValue(), frameType.getType(), msg));

    return packetSender.destroy();
  }

  public Future<Void> close() {
    stateMachine.closeImmediate();

    return packetSender.destroy();
  }

  public Future<Void> closeByPeer() {
    return packetSender.destroy();
  }
}
