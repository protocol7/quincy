package com.protocol7.nettyquick.streams;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.RstStreamFrame;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;
import com.protocol7.nettyquick.protocol.packets.FullPacket;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class Stream {

  public enum StreamType {
    Receiving,
    Sending,
    Bidirectional;

    public boolean canSend() {
      return this == Sending || this == Bidirectional;
    }

    public boolean canReceive() {
      return this == Receiving || this == Bidirectional;
    }
  }

  private final StreamId id;
  private final Connection connection;
  private final StreamListener listener;
  private final AtomicLong offset = new AtomicLong(0);
  private final StreamType streamType;
  private final SendStateMachine sendStateMachine = new SendStateMachine();
  private final ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine();
  private final ReceivedDataBuffer receivedDataBuffer = new ReceivedDataBuffer();

  public Stream(final StreamId id, final Connection connection, final StreamListener listener, StreamType streamType) {
    this.id = id;
    this.connection = connection;
    this.listener = listener;
    this.streamType = streamType;
  }

  public StreamId getId() {
    return id;
  }

  public StreamType getStreamType() {
    return streamType;
  }

  public void write(final byte[] b, boolean finish) {
    canWrite();

    final long frameOffset = offset.getAndAdd(b.length);
    final StreamFrame sf = new StreamFrame(id, frameOffset, finish, b);
    FullPacket p = connection.sendPacket(sf);

    sendStateMachine.onStream(p.getPacketNumber(), finish);
  }

  public void reset(int applicationErrorCode) {
    canReset();

    final Frame frame = new RstStreamFrame(id, applicationErrorCode, offset.get());

    final FullPacket p = connection.sendPacket(frame);

    sendStateMachine.onReset(p.getPacketNumber());
  }

  private void canWrite() {
    if (!streamType.canSend() || !sendStateMachine.canSend()) {
      throw new IllegalStateException();
    }
  }

  private void canReset() {
    if (!sendStateMachine.canReset()) {
      throw new IllegalStateException();
    }
  }

  public void onData(final long offset, final boolean finish, final byte[] b) {
    receivedDataBuffer.onData(b, offset, finish);

    Optional<byte[]> data = receivedDataBuffer.read();
    while (data.isPresent()) {
      listener.onData(this, data.get());
      data = receivedDataBuffer.read();
    }
    if (receivedDataBuffer.isDone()) {
      listener.onDone();
    }

    receiveStateMachine.onStream(finish);
  }

  public void onReset(final int applicationErrorCode, final long offset) {
    receiveStateMachine.onReset();
    listener.onReset(this, applicationErrorCode, offset);
    receiveStateMachine.onAppReadReset();
  }

  public void onAck(PacketNumber pn) {
    sendStateMachine.onAck(pn);
  }

  public boolean isClosed() {
    return !sendStateMachine.canSend() || !receiveStateMachine.canReceive();
  }
}
