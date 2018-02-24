package com.protocol7.nettyquick.server;

import java.net.InetSocketAddress;
import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.protocol.ConnectionId;
import io.netty.channel.Channel;

public class Connections {

  private final Map<ConnectionId, ServerConnection> connections = Maps.newConcurrentMap();

  public ServerConnection getOrCreate(ConnectionId connId, StreamListener streamHandler, Channel channel, InetSocketAddress clientAddress) {
    ServerConnection conn = connections.get(connId);
    if (conn == null) {
      conn = ServerConnection.create(streamHandler, channel, clientAddress);
      ServerConnection existingConn = connections.putIfAbsent(connId, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }
}
