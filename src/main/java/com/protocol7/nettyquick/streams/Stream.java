package com.protocol7.nettyquick.streams;

import com.protocol7.nettyquick.Connection;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.ShortPacket;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;

public class Stream {

  private final StreamId id;
  private final Connection connection;
  private final StreamListener handler;

  public Stream(final StreamId id, final Connection connection, final StreamListener handler) {
    this.id = id;
    this.connection = connection;
    this.handler = handler;
  }

  public void write(byte[] b) {
    StreamFrame sf = new StreamFrame(id, 0, true, b);
    Payload payload = new Payload(sf);

    Packet p = new ShortPacket(false, false, PacketType.Four_octets, connection.getConnectionId(), connection.nextPacketNumber(), payload);

    connection.sendPacket(p);
  }

  public void onData(byte[] b) {
    handler.onData(this, b);
  }
}
