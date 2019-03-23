package com.protocol7.nettyquic.streams;

import com.protocol7.nettyquic.connection.FrameSender;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.streams.Stream.StreamType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Streams {

  private final FrameSender frameSender;
  private final Map<StreamId, Stream> streams = new ConcurrentHashMap<>();
  private StreamId maxId = new StreamId(0);

  public Streams(final FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  public Stream openStream(
      final boolean client, final boolean bidirectional, final StreamListener handler) {
    final StreamType type = bidirectional ? StreamType.Bidirectional : StreamType.Sending;
    final StreamId streamId = StreamId.next(maxId, client, bidirectional);
    this.maxId = streamId;
    final Stream stream = new Stream(streamId, frameSender, handler, type);
    streams.put(streamId, stream);
    return stream;
  }

  public Stream getOrCreate(final StreamId streamId, final StreamListener handler) {
    Stream stream = streams.get(streamId);
    if (stream == null) {
      stream =
          new Stream(
              streamId, frameSender, handler, StreamType.Bidirectional); // TODO support stream type
      Stream existingStream = streams.putIfAbsent(streamId, stream);
      if (existingStream != null) {
        stream = existingStream;
      }
    }
    return stream;
  }

  public void onAck(final PacketNumber pn) {
    for (final Stream stream : streams.values()) {
      stream.onAck(pn);
    }
  }
}
