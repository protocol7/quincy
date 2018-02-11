package com.protocol7.nettyquick.client;

import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.protocol.StreamId;

public class ClientStreams {
  private final Map<StreamId, ClientStream> streams = Maps.newConcurrentMap();

  public ClientStream getOrCreate(StreamId streamId, ClientConnection connection, StreamListener listener) {
    ClientStream conn = streams.get(streamId);
    if (conn == null) {
      conn = new ClientStream(streamId, connection, listener);
      ClientStream existingConn = streams.putIfAbsent(streamId, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }

}
