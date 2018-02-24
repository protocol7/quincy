package com.protocol7.nettyquick.streams;

import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.StreamId;

public class Streams {

  private final Connection connection;
  private final Map<StreamId, Stream> streams = Maps.newConcurrentMap();

  public Streams(final Connection connection) {
    this.connection = connection;
  }

  public Stream getOrCreate(StreamId streamId, StreamListener handler) {
    Stream stream = streams.get(streamId);
    if (stream == null) {
      stream = new Stream(streamId, connection, handler);
      Stream existingStream = streams.putIfAbsent(streamId, stream);
      if (existingStream != null) {
        stream = existingStream;
      }
    }
    return stream;
  }

}
