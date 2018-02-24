package com.protocol7.nettyquick.streams;

import java.util.concurrent.atomic.AtomicLong;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.connection.Sender;
import com.protocol7.nettyquick.protocol.ShortPacket;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;

public class Stream {

  private final StreamId id;
  private final Connection connection;
  private final StreamListener handler;
  private final AtomicLong offset = new AtomicLong(0);

  public Stream(final StreamId id, final Connection connection, final StreamListener handler) {
    this.id = id;
    this.connection = connection;
    this.handler = handler;
  }

  public void write(byte[] b) {
    long frameOffset = offset.getAndAdd(b.length);
    StreamFrame sf = new StreamFrame(id, frameOffset, false, b);
    Payload payload = new Payload(sf);

    Packet p = new ShortPacket(false,
                               false,
                               PacketType.Four_octets,
                               connection.getConnectionId(),
                               connection.nextPacketNumber(),
                               payload);

    connection.sendPacket(p);
  }

  public void onData(long offset, byte[] b) {
    handler.onData(this, offset, b);
  }
}
