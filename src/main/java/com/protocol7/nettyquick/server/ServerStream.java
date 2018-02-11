package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.protocol.LongPacket;
import com.protocol7.nettyquick.protocol.LongPacketType;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;

public class ServerStream {

  private final StreamId id;
  private final Connection connection;
  private final StreamHandler handler;

  public ServerStream(final StreamId id, final Connection connection, final StreamHandler handler) {
    this.id = id;
    this.connection = connection;
    this.handler = handler;
  }

  public void write(byte[] b) {
    StreamFrame sf = new StreamFrame(id, 0, true, b);
    Payload payload = new Payload(sf);

    Packet p = new LongPacket(LongPacketType.Zero_RTT_Protected, connection.getConnectionId(), connection.getVersion(), connection.nextPacketNumber(), payload);

    connection.sendPacket(p);
  }

  public void onData(byte[] b) {
    handler.onData(this, b);
  }
}
