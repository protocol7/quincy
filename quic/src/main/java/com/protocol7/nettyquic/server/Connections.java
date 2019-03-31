package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.connection.Connection;
import com.protocol7.nettyquic.connection.PacketSender;
import com.protocol7.nettyquic.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.streams.StreamListener;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connections {

  private final Logger log = LoggerFactory.getLogger(Connections.class);

  private final Version version;
  private final List<byte[]> certificates;
  private final PrivateKey privateKey;
  private final Map<ConnectionId, ServerConnection> connections = new ConcurrentHashMap<>();

  public Connections(
      final Version version, final List<byte[]> certificates, final PrivateKey privateKey) {
    this.version = version;
    this.certificates = certificates;
    this.privateKey = privateKey;
  }

  public ServerConnection get(
      final Optional<ConnectionId> connIdOpt,
      final StreamListener streamHandler,
      final PacketSender packetSender,
      final InetSocketAddress peerAddress) {

    final ConnectionId connId = connIdOpt.orElse(ConnectionId.random());

    ServerConnection conn = connections.get(connId);
    if (conn == null) {
      log.debug("Creating new server connection for {}", connId);
      conn =
          new ServerConnection(
              version,
              connId,
              streamHandler,
              packetSender,
              certificates,
              privateKey,
              new DefaultFlowControlHandler(49152, 32768),
              peerAddress);
      final ServerConnection existingConn = connections.putIfAbsent(connId, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }

  public Optional<Connection> get(final ConnectionId connId) {
    return Optional.ofNullable(connections.get(connId));
  }
}
