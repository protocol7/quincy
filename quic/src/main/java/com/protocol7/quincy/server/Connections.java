package com.protocol7.quincy.server;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.streams.StreamListener;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connections {

  private final Logger log = LoggerFactory.getLogger(Connections.class);

  private final Configuration configuration;
  private final List<byte[]> certificates;
  private final PrivateKey privateKey;
  private final Map<ConnectionId, ServerConnection> connections = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler;

  public Connections(
      final Configuration configuration,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final ScheduledExecutorService scheduler) {
    this.configuration = configuration;
    this.certificates = certificates;
    this.privateKey = privateKey;
    this.scheduler = scheduler;
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
              configuration,
              connId,
              streamHandler,
              packetSender,
              certificates,
              privateKey,
              new DefaultFlowControlHandler(
                  configuration.getInitialMaxData(), configuration.getInitialMaxStreamDataUni()),
              peerAddress,
              scheduler);
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
