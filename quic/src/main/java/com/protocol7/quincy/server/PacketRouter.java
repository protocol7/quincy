package com.protocol7.quincy.server;

import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HalfParsedPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.streams.StreamListener;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.slf4j.MDC;

public class PacketRouter {

  private final Version version;
  private final Connections connections;
  private final StreamListener listener;

  public PacketRouter(
      final Version version, final Connections connections, final StreamListener listener) {
    this.version = version;
    this.connections = connections;
    this.listener = listener;
  }

  private boolean validateVersion(
      final HalfParsedPacket<?> halfParsed,
      final PacketSender sender,
      final ConnectionId srcConnId) {

    if (halfParsed.getVersion().isPresent()) {
      if (halfParsed.getVersion().get() != version) {
        final VersionNegotiationPacket verNeg =
            new VersionNegotiationPacket(
                halfParsed.getDestinationConnectionId(), srcConnId, version);
        sender.send(verNeg);
        return false;
      }
    }
    return true;
  }

  public void route(
      final ByteBuf bb, final PacketSender sender, final InetSocketAddress peerAddress) {

    while (bb.isReadable()) {
      final HalfParsedPacket<?> halfParsed = Packet.parse(bb, ConnectionId.LENGTH);

      final Connection conn =
          connections.get(halfParsed.getDestinationConnectionId(), listener, sender, peerAddress);

      if (validateVersion(halfParsed, sender, conn.getSourceConnectionId())) {
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
