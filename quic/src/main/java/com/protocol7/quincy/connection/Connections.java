package com.protocol7.quincy.connection;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
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
  private final Map<ConnectionId, Connection> connections =
      new ConcurrentHashMap<>(); // dcid -> connection
  private final Timer timer;

  public Connections(final Configuration configuration, final Timer timer) {
    this.configuration = configuration;
    this.timer = timer;
  }

  public Connection create(
      final ConnectionId dcid,
      final ConnectionId scid,
      final ConnectionId originalRemoteConnectionId,
      final StreamHandler streamHandler,
      final PacketSender packetSender,
      final InetSocketAddress peerAddress,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {
    requireNonNull(dcid);
    requireNonNull(scid);
    requireNonNull(originalRemoteConnectionId);
    requireNonNull(streamHandler);
    requireNonNull(packetSender);
    requireNonNull(peerAddress);
    requireNonNull(certificates);
    requireNonNull(privateKey);

    if (connections.containsKey(dcid)) {
      throw new IllegalStateException("Connection already exist");
    }

    log.debug("Creating new server connection for {}", dcid);
    return connections.computeIfAbsent(
        dcid,
        connectionId ->
            Connection.forServer(
                configuration,
                dcid,
                scid,
                originalRemoteConnectionId,
                streamHandler,
                packetSender,
                certificates,
                privateKey,
                new DefaultFlowControlHandler(
                    configuration.getInitialMaxData(), configuration.getInitialMaxStreamDataUni()),
                peerAddress,
                timer));
  }

  public Optional<Connection> get(final ConnectionId dcid) {
    return Optional.ofNullable(connections.get(dcid));
  }

  public void putConnection(final ConnectionId dcid, final Connection connection) {
    log.debug("Adding connection for {}", dcid);
    connections.putIfAbsent(dcid, connection);
  }
}
