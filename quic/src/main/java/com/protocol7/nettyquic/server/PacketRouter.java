package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.connection.PacketSender;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.HalfParsedPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.VersionNegotiationPacket;
import com.protocol7.nettyquic.streams.StreamListener;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.slf4j.MDC;

public class PacketRouter {

  private final Version version;
  private final Connections connections;
  private final StreamListener listener;

  public PacketRouter(Version version, Connections connections, StreamListener listener) {
    this.version = version;
    this.connections = connections;
    this.listener = listener;
  }

  private boolean validateVersion(
      HalfParsedPacket<?> halfParsed, PacketSender sender, Optional<ConnectionId> srcConnId) {

    if (halfParsed.getVersion().isPresent()) {
      if (halfParsed.getVersion().get() != version) {
        VersionNegotiationPacket verNeg =
            new VersionNegotiationPacket(halfParsed.getConnectionId(), srcConnId, Version.CURRENT);
        sender.send(verNeg, null); // TODO remove null
        return false;
      }
    }
    return true;
  }

  public void route(ByteBuf bb, PacketSender sender) {

    while (bb.isReadable()) {
      HalfParsedPacket<?> halfParsed = Packet.parse(bb, -1);

      ServerConnection conn =
          connections.get(
              halfParsed.getConnectionId(),
              listener,
              sender); // TODO fix for when connId is omitted

      if (validateVersion(halfParsed, sender, conn.getLocalConnectionId())) {

        Packet packet = halfParsed.complete(conn::getAEAD);

        MDC.put("actor", "server");
        if (packet instanceof FullPacket) {
          MDC.put("packetnumber", ((FullPacket) packet).getPacketNumber().toString());
        }
        if (packet.getDestinationConnectionId().isPresent()) {
          MDC.put("connectionid", packet.getDestinationConnectionId().get().toString());
        }

        conn.onPacket(packet);
      } else {
        // skip rest of datagram
        break;
      }
    }
  }
}
