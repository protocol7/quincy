package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.connection.PacketSender;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.HalfParsedPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.VersionNegotiationPacket;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.utils.Debug;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
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
      HalfParsedPacket<?> halfParsed,
      PacketSender sender,
      Optional<ConnectionId> srcConnId,
      InetSocketAddress clientAddress) {

    System.out.println(halfParsed.getVersion());
    if (halfParsed.getVersion().isPresent()) {
      if (halfParsed.getVersion().get() != version) {
        VersionNegotiationPacket verNeg =
            new VersionNegotiationPacket(halfParsed.getConnectionId(), srcConnId, Version.CURRENT);
        System.out.println(111);
        sender.send(verNeg, clientAddress, null); // TODO remove null
        return false;
      }
    }
    return true;
  }

  public void route(ByteBuf bb, InetSocketAddress clientAddress, PacketSender sender) {

    while (bb.isReadable()) {
      Debug.buffer(bb);
      HalfParsedPacket<?> halfParsed = Packet.parse(bb, -1);

      ServerConnection conn =
          connections.get(
              halfParsed.getConnectionId(),
              listener,
              sender,
              clientAddress); // TODO fix for when connId is omitted

      if (validateVersion(halfParsed, sender, conn.getSourceConnectionId(), clientAddress)) {

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
