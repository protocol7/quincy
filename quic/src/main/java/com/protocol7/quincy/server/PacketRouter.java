package com.protocol7.quincy.server;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HalfParsedPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.slf4j.MDC;

public class PacketRouter {

  private final Version version;
  private final Connections connections;
  private final StreamHandler listener;

  public PacketRouter(
      final Version version, final Connections connections, final StreamHandler listener) {
    this.version = version;
    this.connections = connections;
    this.listener = listener;
  }

  private boolean validateVersion(
      final HalfParsedPacket<?> halfParsed,
      final PacketSender sender,
      final ConnectionId srcConnId,
      final AEAD aead) {

    if (halfParsed.getVersion().isPresent()) {
      if (halfParsed.getVersion().get() != version) {
        final VersionNegotiationPacket verNeg =
            new VersionNegotiationPacket(
                halfParsed.getDestinationConnectionId(), srcConnId, version);
        sender.send(verNeg, aead);
        return false;
      }
    }
    return true;
  }

  public void route(
      final ByteBuf bb, final PacketSender sender, final InetSocketAddress peerAddress) {
    requireNonNull(bb);
    requireNonNull(sender);
    requireNonNull(peerAddress);

    while (bb.isReadable()) {
      final HalfParsedPacket<?> halfParsed = Packet.parse(bb, ConnectionId.LENGTH);

      final Connection conn =
          connections.get(
              halfParsed.getDestinationConnectionId(),
              halfParsed.getSourceConnectionId(),
              listener,
              sender,
              peerAddress);

      if (validateVersion(
          halfParsed, sender, conn.getLocalConnectionId(), conn.getAEAD(EncryptionLevel.Initial))) {
        final Packet packet = halfParsed.complete(conn::getAEAD);

        MDC.put("actor", "server");
        if (packet instanceof FullPacket) {
          MDC.put("packetnumber", Long.toString(((FullPacket) packet).getPacketNumber()));
        }
        MDC.put("connectionid", packet.getDestinationConnectionId().toString());

        conn.onPacket(packet);
      } else {
        // skip rest of datagram
        break;
      }
    }
  }
}
