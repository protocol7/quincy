package com.protocol7.nettyquick.server;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.streams.StreamListener;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;

public class Connections {

  private final Logger log = LoggerFactory.getLogger(Connections.class);

  private final Map<ConnectionId, ServerConnection> connections = Maps.newConcurrentMap();

  public ServerConnection get(Optional<ConnectionId> connIdOpt, StreamListener streamHandler, Channel channel, InetSocketAddress clientAddress) {

    ConnectionId connId = connIdOpt.orElse(ConnectionId.random());

    ServerConnection conn = connections.get(connId);
    if (conn == null) {
      log.debug("Creating new server connection for {}", connId);
      conn = ServerConnection.create(streamHandler, channel, clientAddress, connId);
      ServerConnection existingConn = connections.putIfAbsent(connId, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }

  public Optional<Connection> get(ConnectionId connId) {
    return Optional.ofNullable(connections.get(connId));
  }
}
