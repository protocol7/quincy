package com.protocol7.quincy.connection;

import static com.protocol7.quincy.connection.State.Closed;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.protocol7.quincy.Pipeline;
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
import com.protocol7.quincy.tls.EncryptionLevel;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractConnection implements Connection {

  private final Version version;

  private final InetSocketAddress peerAddress;

  private Optional<ConnectionId> destinationConnectionId = empty();
  private final ConnectionId sourceConnectionId;

  protected StateMachine stateMachine;
  private final PacketSender packetSender;

  private Optional<byte[]> token = Optional.empty();

  private final AtomicReference<Long> sendPacketNumber = new AtomicReference<>(-1L);

  protected AbstractConnection(
      final Version version,
      final InetSocketAddress peerAddress,
      final ConnectionId sourceConnectionId,
      final PacketSender packetSender) {
    this.version = version;
    this.peerAddress = peerAddress;
    this.sourceConnectionId = sourceConnectionId;
    this.packetSender = packetSender;
  }

  protected abstract Pipeline getPipeline();

  public Packet sendPacket(final Packet p) {
    if (stateMachine.getState() == Closed) {
      throw new IllegalStateException("Connection not open");
    }

    final Packet newPacket = getPipeline().send(this, p);

    // check again if any handler closed the connection
    if (stateMachine.getState() == Closed) {
      throw new IllegalStateException("Connection not open");
    }

    sendPacketUnbuffered(newPacket);
    return newPacket;
  }

  private void sendPacketUnbuffered(final Packet packet) {
    packetSender.send(packet).awaitUninterruptibly(); // TODO fix
  }

  public FullPacket send(final EncryptionLevel level, final Frame... frames) {
    if (destinationConnectionId.isEmpty()) {
      throw new IllegalStateException("Can send when remote connection ID is unknown");
    }
    final ConnectionId remoteConnectionId = destinationConnectionId.get();

    final Packet packet;
    if (level == EncryptionLevel.OneRtt) {
      packet =
          ShortPacket.create(
              false, remoteConnectionId, sourceConnectionId, nextSendPacketNumber(), frames);
    } else if (level == EncryptionLevel.Handshake) {
      packet =
          HandshakePacket.create(
              remoteConnectionId, sourceConnectionId, nextSendPacketNumber(), version, frames);
    } else {
      packet =
          InitialPacket.create(
              remoteConnectionId,
              sourceConnectionId,
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

  public ConnectionId getSourceConnectionId() {
    return sourceConnectionId;
  }

  public void setRemoteConnectionId(final ConnectionId remoteConnectionId) {
    this.destinationConnectionId = Optional.of(remoteConnectionId);
  }

  public State getState() {
    return stateMachine.getState();
  }

  public void setState(final State state) {
    stateMachine.setState(state);
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  public void setToken(final byte[] token) {
    this.token = of(token);
  }

  private long nextSendPacketNumber() {
    return sendPacketNumber.updateAndGet(packetNumber -> PacketNumber.next(packetNumber));
  }

  public void resetSendPacketNumber() {
    sendPacketNumber.set(-1L);
  }

  public Future<Void> close(
      final TransportError error, final FrameType frameType, final String msg) {
    stateMachine.closeImmediate(new ConnectionCloseFrame(error.getValue(), frameType, msg));

    return packetSender.destroy();
  }

  public Future<Void> close() {
    stateMachine.closeImmediate();

    return packetSender.destroy();
  }

  public void closeByPeer() {
    packetSender.destroy().awaitUninterruptibly(); // TOOD fix
  }
}
