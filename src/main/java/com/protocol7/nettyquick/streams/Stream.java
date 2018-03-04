package com.protocol7.nettyquick.streams;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.ShortPacket;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.RstStreamFrame;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;

public class Stream {

  private final StreamId id;
  private final Connection connection;
  private final StreamListener listener;
  private final AtomicLong offset = new AtomicLong(0);
  private final AtomicBoolean closed = new AtomicBoolean(false);

  public Stream(final StreamId id, final Connection connection, final StreamListener listener) {
    this.id = id;
    this.connection = connection;
    this.listener = listener;
  }

  public void write(final byte[] b, boolean finish) {
    verifyOpen();

    final long frameOffset = offset.getAndAdd(b.length);
    final StreamFrame sf = new StreamFrame(id, frameOffset, finish, b);
    final Payload payload = new Payload(sf);

    final Packet p = new ShortPacket(false,
                               false,
                               PacketType.Four_octets,
                               connection.getConnectionId(),
                               connection.nextSendPacketNumber(),
                               payload);

    connection.sendPacket(p);

    closed.compareAndSet(false, finish);
  }

  public void reset(int applicationErrorCode) {
    verifyOpen();

    final Frame frame = new RstStreamFrame(id, applicationErrorCode, offset.get());

    final Packet p = new ShortPacket(false,
                                     false,
                                     PacketType.Four_octets,
                                     connection.getConnectionId(),
                                     connection.nextSendPacketNumber(),
                                     new Payload(frame));

    connection.sendPacket(p);
    closed.set(true);
  }

  private void verifyOpen() {
    if (closed.get()) {
      throw new IllegalStateException("Stream closed");
    }
  }

  public void onData(final long offset, final byte[] b) {
    listener.onData(this, offset, b);
  }

  public void onReset(final int applicationErrorCode, final long offset) {
    closed.set(true);
    listener.onReset(this, applicationErrorCode, offset);
  }

  public boolean isClosed() {
    return closed.get();
  }
}
