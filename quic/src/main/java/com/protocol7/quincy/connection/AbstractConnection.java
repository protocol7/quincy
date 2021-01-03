package com.protocol7.quincy.connection;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.tls.EncryptionLevel;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractConnection implements Connection {

  private final Version version;

  private Optional<ConnectionId> destinationConnectionId = empty();
  private final ConnectionId sourceConnectionId;

  private Optional<byte[]> token = Optional.empty();

  private final AtomicReference<Long> sendPacketNumber = new AtomicReference<>(-1L);

  protected AbstractConnection(final Version version, final ConnectionId sourceConnectionId) {
    this.version = version;
    this.sourceConnectionId = sourceConnectionId;
  }

  public FullPacket send(final EncryptionLevel level, final Frame... frames) {
    if (getDestinationConnectionId().isEmpty()) {
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

  public Optional<ConnectionId> getDestinationConnectionId() {
    return destinationConnectionId;
  }

  public ConnectionId getSourceConnectionId() {
    return sourceConnectionId;
  }

  public void setRemoteConnectionId(final ConnectionId remoteConnectionId) {
    this.destinationConnectionId = Optional.of(remoteConnectionId);
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
}
