package com.protocol7.quincy.streams;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.protocol.StreamId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Streams {

  private final FrameSender frameSender;
  private final Map<Long, DefaultStream> streams = new ConcurrentHashMap<>();
  private long maxId = -1;

  public Streams(final FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  public Stream openStream(
      final boolean client, final boolean bidirectional, final StreamHandler handler) {
    final StreamType type = bidirectional ? StreamType.Bidirectional : StreamType.Sending;
    final long streamId = StreamId.next(maxId, client, bidirectional);
    this.maxId = streamId;
    final DefaultStream stream = new DefaultStream(streamId, frameSender, handler, type);
    streams.put(streamId, stream);
    return stream;
  }

  public DefaultStream getOrCreate(final long streamId, final StreamHandler handler) {
    DefaultStream stream = streams.get(streamId);
    if (stream == null) {
      stream =
          new DefaultStream(
              streamId, frameSender, handler, StreamType.Bidirectional); // TODO support stream type
      final DefaultStream existingStream = streams.putIfAbsent(streamId, stream);
      if (existingStream != null) {
        stream = existingStream;
      }
    }
    return stream;
  }

  public void onAck(final long pn) {
    for (final DefaultStream stream : streams.values()) {
      stream.onAck(pn);
    }
  }
}
