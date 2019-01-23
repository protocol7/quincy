package com.protocol7.nettyquic.streams;

import com.google.common.collect.Maps;
import com.protocol7.nettyquic.Config;
import com.protocol7.nettyquic.connection.Connection;
import com.protocol7.nettyquic.flow.FlowController;
import com.protocol7.nettyquic.protocol.PacketBuffer;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.TransportParameters;
import com.protocol7.nettyquic.streams.Stream.StreamType;
import java.util.Map;
import java.util.function.Supplier;

public class Streams implements PacketBuffer.AckListener {

  private final Connection connection;
  private final Map<StreamId, Stream> streams = Maps.newConcurrentMap();
  private long maxId = -1;
  private final Supplier<TransportParameters> transportParameters;
  private final boolean client;

  public Streams(final Connection connection, final Supplier<TransportParameters> transportParameters, final boolean client) {
    this.connection = connection;
    this.transportParameters = transportParameters;
    this.client = client;
  }

  public StreamInterface openStream(boolean bidirectional, StreamListener handler) {
    StreamType type = bidirectional ? StreamType.Bidirectional : StreamType.Sending;
    StreamId streamId = StreamId.next(maxId, client, bidirectional);
    maxId = Math.max(maxId, streamId.getValue());
    Stream stream = new Stream(streamId, connection, handler, type, createFlowController(streamId.isBidirectional(), streamId.isClient() && client));
    streams.put(streamId, stream);
    return stream;
  }

  public StreamInterface getOrCreate(StreamId streamId, StreamListener handler) {
    Stream stream = streams.get(streamId);
    maxId = Math.max(maxId, streamId.getValue());
    if (stream == null) {

      stream =
          new Stream(
              streamId, connection, handler, StreamType.Bidirectional, createFlowController(streamId.isBidirectional())); // TODO support stream type
      Stream existingStream = streams.putIfAbsent(streamId, stream);
      if (existingStream != null) {
        stream = existingStream;
      }
    }
    return stream;
  }

  private FlowController createFlowController(boolean bidi, boolean local) {
    TransportParameters remoteTp = transportParameters.get();
    int readMax;
    int writeMax;
    if (bidi) {
      writeMax = Config.INITIAL_MAX_BIDI_STREAMS;

      int tpReadMax;
      int tpWriteMax;
      if (local) {
        tpReadMax = remoteTp.getInitialMaxStreamDataBidiRemote();
        tpWriteMax = remoteTp.getInitialMaxStreamDataBidiLocal();
      } else {
        tpReadMax = remoteTp.getInitialMaxStreamDataBidiLocal();
        tpWriteMax = remoteTp.getInitialMaxStreamDataBidiRemote();
      }
      readMax = Math.min(Config.INITIAL_MAX_BIDI_STREAMS, tpReadMax); // TODO is this correct?
      writeMax = Math.min(Config.INITIAL_MAX_BIDI_STREAMS, tpWriteMax); // TODO is this correct?
    } else {
      readMax = Math.min(Config.INITIAL_MAX_UNI_STREAMS, remoteTp.getInitialMaxStreamDataUni()); // TODO is this correct?
      writeMax = readMax;
    }

    final Supplier<FlowController> flowControllerSupplier = new Supplier<FlowController>() {
      @Override
      public FlowController get() {
        return new FlowController(
                readMax,
                writeMax

        );
      }
    };

  }

  public void onAck(PacketNumber pn) {
    for (Stream stream : streams.values()) {
      stream.onAck(pn);
    }
  }
}
