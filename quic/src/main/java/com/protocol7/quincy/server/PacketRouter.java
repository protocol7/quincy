package com.protocol7.quincy.server;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.addressvalidation.QuicTokenHandler;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HalfParsedPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.PacketType;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.streams.StreamHandler;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class PacketRouter {

  private final Logger log = LoggerFactory.getLogger(PacketRouter.class);

  private final Version version;
  private final Connections connections;
  private final StreamHandler listener;
  private final QuicTokenHandler tokenHandler;

  public PacketRouter(
      final Version version,
      final Connections connections,
      final StreamHandler listener,
      final QuicTokenHandler tokenHandler) {
    this.version = version;
    this.connections = connections;
    this.listener = listener;
    this.tokenHandler = tokenHandler;
  }

  private boolean validateVersion(final HalfParsedPacket<?> halfParsed, final PacketSender sender) {

    if (halfParsed.getVersion().isPresent()) {
      if (halfParsed.getVersion().get() != version) {
        final VersionNegotiationPacket verNeg =
            new VersionNegotiationPacket(
                halfParsed.getSourceConnectionId().get(),
                halfParsed.getDestinationConnectionId(),
                version);

        sender.send(verNeg, null);
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

      log.debug(
          "Routing packet of type {}, dcid={}, scid={}",
          halfParsed.getType(),
          halfParsed.getDestinationConnectionId(),
          halfParsed.getSourceConnectionId());

      if (validateVersion(halfParsed, sender)) {
        // if the connection already exist, use it
        Optional<Connection> connection = connections.get(halfParsed.getDestinationConnectionId());

        if (connection.isEmpty()) {
          log.debug("Connection does not exist, verify packet for creation of new connection");
          // else, if initial packet, check for retry token

          if (halfParsed.getType() == PacketType.Initial) {
            if (halfParsed.getRetryToken().isPresent()) {
              // validate retry packet, get original dcid
              log.debug("Initial packet with retry token, validate");

              final Optional<ConnectionId> originalConnId =
                  tokenHandler.validateToken(halfParsed.getRetryToken().get(), peerAddress);

              if (originalConnId.isPresent()) {
                // validation succeeded, create connection
                log.debug("Retry token validated, creating new connection");

                connection =
                    Optional.of(
                        connections.create(
                            halfParsed.getDestinationConnectionId(),
                            halfParsed.getSourceConnectionId(),
                            originalConnId.get(),
                            listener,
                            sender,
                            peerAddress));
              } else {
                // token validation failed, drop
                log.warn("Invalid retry token, dropping packet");
              }
            } else {
              // send retry packet
              log.debug("No retry token, send retry packet");

              final byte[] retryToken =
                  tokenHandler.writeToken(halfParsed.getDestinationConnectionId(), peerAddress);

              final ConnectionId newLocalConnectionId = ConnectionId.random();

              sender.send(
                  RetryPacket.createOutgoing(
                      halfParsed.getVersion().get(),
                      halfParsed.getSourceConnectionId().get(),
                      newLocalConnectionId,
                      halfParsed.getDestinationConnectionId(),
                      retryToken),
                  null);

              log.debug("Retry packet sent");
            }
          } else {
            // drop
            log.warn("Non-initial packet sent without matching connection, will drop");
          }
        }

        if (connection.isPresent()) {
          log.debug("Connection found, finishing parsing and routing packet");
          final Connection conn = connection.get();

          final Packet packet = halfParsed.complete(conn::getAEAD);

          MDC.put("actor", "server");
          if (packet instanceof FullPacket) {
            MDC.put("packetnumber", Long.toString(((FullPacket) packet).getPacketNumber()));
          }
          MDC.put("connectionid", packet.getDestinationConnectionId().toString());

          conn.onPacket(packet);
        } else {
          // invalid data, skip rest of datagram
          break;
        }

      } else {
        // invalid data, skip rest of datagram
        break;
      }
    }
  }
}
