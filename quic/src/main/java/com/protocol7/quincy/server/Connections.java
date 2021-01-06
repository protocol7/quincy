package com.protocol7.quincy.server;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.connection.AbstractConnection;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.PacketSender;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.quincy.netty2.api.QuicTokenHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.streams.StreamHandler;
import io.netty.util.Timer;
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

  private final Configuration configuration;
  private final List<byte[]> certificates;
  private final PrivateKey privateKey;
  private final Map<ConnectionId, Connection> connections = new ConcurrentHashMap<>();
  private final Timer timer;
  private final QuicTokenHandler tokenHandler;

  public Connections(
      final Configuration configuration,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final Timer timer,
      final QuicTokenHandler tokenHandler) {
    this.configuration = configuration;
    this.certificates = certificates;
    this.privateKey = privateKey;
    this.timer = timer;
    this.tokenHandler = tokenHandler;
  }

  public Connection get(
      final ConnectionId dcid,
      final StreamHandler streamHandler,
      final PacketSender packetSender,
      final InetSocketAddress peerAddress) {

    Connection conn = connections.get(dcid);
    if (conn == null) {
      log.debug("Creating new server connection for {}", dcid);
      conn =
          AbstractConnection.forServer(
              configuration,
              dcid,
              streamHandler,
              packetSender,
              certificates,
              privateKey,
              new DefaultFlowControlHandler(
                  configuration.getInitialMaxData(), configuration.getInitialMaxStreamDataUni()),
              peerAddress,
              timer,
              tokenHandler);
      final Connection existingConn = connections.putIfAbsent(dcid, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }

  public Optional<Connection> get(final ConnectionId dcid) {
    return Optional.ofNullable(connections.get(dcid));
  }
}
