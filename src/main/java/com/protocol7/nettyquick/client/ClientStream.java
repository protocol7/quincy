package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.protocol.LongPacket;
import com.protocol7.nettyquick.protocol.LongPacketType;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;

public class ClientStream {

  private final StreamId streamId;
  private final ClientConnection connection;
  private final StreamListener listener;

  public ClientStream(final StreamId streamId, final ClientConnection connection, final StreamListener listener) {
    this.streamId = streamId;
    this.connection = connection;
    this.listener = listener;
  }

  public void write(byte[] b) {
    // TODO create stream frame
    StreamFrame sf = new StreamFrame(streamId, 0, true, b);
    Payload payload = new Payload(sf);

    Packet p = new LongPacket(LongPacketType.Zero_RTT_Protected, connection.getConnectionId(), connection.getVersion(), connection.nextPacketNumber(), payload);

    connection.sendPacket(p);
  }

  public void onData(byte[] b) {
    listener.onData(b);
  }
}
