package com.protocol7.nettyquick.server;

import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.protocol.StreamId;

public class ServerStreams {
  private final Map<StreamId, ServerStream> streams = Maps.newConcurrentMap();

  public ServerStream getOrCreate(StreamId streamId, ServerConnection connection, StreamHandler handler) {
    ServerStream conn = streams.get(streamId);
    if (conn == null) {
      conn = new ServerStream(streamId, connection, handler);
      ServerStream existingConn = streams.putIfAbsent(streamId, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }

}
