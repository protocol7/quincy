package com.protocol7.nettyquick.streams;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.PacketBuffer;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.streams.Stream.StreamType;

import java.util.Map;

public class Streams implements PacketBuffer.AckListener {

  private final Connection connection;
  private final Map<StreamId, Stream> streams = Maps.newConcurrentMap();
  private StreamId maxId = new StreamId(0);

  public Streams(final Connection connection) {
    this.connection = connection;
  }

  public Stream openStream(boolean client, boolean bidirectional, StreamListener handler) {
    StreamType type = bidirectional ? StreamType.Bidirectional : StreamType.Sending;
    StreamId streamId = StreamId.next(maxId, client, bidirectional);
    this.maxId = streamId;
    Stream stream = new Stream(streamId,
                               connection,
                               handler,
                               type);
    streams.put(streamId, stream);
    return stream;
  }

  public Stream getOrCreate(StreamId streamId, StreamListener handler) {
    Stream stream = streams.get(streamId);
    if (stream == null) {
      stream = new Stream(streamId, connection, handler, StreamType.Bidirectional); // TODO support stream type
      Stream existingStream = streams.putIfAbsent(streamId, stream);
      if (existingStream != null) {
        stream = existingStream;
      }
    }
    return stream;
  }

  public void onAck(PacketNumber pn) {
    for (Stream stream : streams.values()) {
      stream.onAck(pn);
    }
  }

}
