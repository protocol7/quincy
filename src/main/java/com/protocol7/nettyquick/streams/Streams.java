package com.protocol7.nettyquick.streams;

import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.StreamId;

public class Streams {
  private final Map<StreamId, Stream> streams = Maps.newConcurrentMap();

  public Stream getOrCreate(StreamId streamId, Connection connection, StreamListener handler) {
    Stream conn = streams.get(streamId);
    if (conn == null) {
      conn = new Stream(streamId, connection, handler);
      Stream existingConn = streams.putIfAbsent(streamId, conn);
      if (existingConn != null) {
        conn = existingConn;
      }
    }
    return conn;
  }

}
