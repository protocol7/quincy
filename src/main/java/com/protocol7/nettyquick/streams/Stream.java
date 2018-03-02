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

  public Stream(final StreamId id, final Connection connection, final StreamListener listener) {
    this.id = id;
    this.connection = connection;
    this.handler = listener;
  }

  public void write(final byte[] b) {
    final long frameOffset = offset.getAndAdd(b.length);
    final StreamFrame sf = new StreamFrame(id, frameOffset, false, b);
    final Payload payload = new Payload(sf);

    final Packet p = new ShortPacket(false,
                               false,
                               PacketType.Four_octets,
                               connection.getConnectionId(),
                               connection.nextSendPacketNumber(),
                               payload);

    connection.sendPacket(p);
  }

  public void onData(final long offset, final byte[] b) {
    handler.onData(this, offset, b);
  }
}
