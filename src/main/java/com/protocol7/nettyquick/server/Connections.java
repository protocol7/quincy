package com.protocol7.nettyquick.server;

import java.net.InetSocketAddress;
import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.protocol.ConnectionId;
import io.netty.channel.Channel;

public class Connections {

  private final Map<ConnectionId, Connection> connections = Maps.newConcurrentMap();

  public Connection getOrCreate(ConnectionId connId, StreamHandler streamHandler, Channel channel, InetSocketAddress clientAddress) {
    Connection conn = connections.get(connId);
    if (conn == null) {
      conn = Connection.create(streamHandler, channel, clientAddress);
      Connection existingConn = connections.putIfAbsent(connId, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }
}
