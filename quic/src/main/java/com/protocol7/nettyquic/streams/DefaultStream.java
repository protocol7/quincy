package com.protocol7.nettyquic.streams;

import com.protocol7.nettyquic.FrameSender;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.ResetStreamFrame;
import com.protocol7.nettyquic.protocol.frames.StreamFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultStream implements Stream {

  private final StreamId id;
  private final FrameSender sender;
  private final StreamListener listener;
  private final AtomicLong offset = new AtomicLong(0);
  private final StreamType streamType;
  private final SendStateMachine sendStateMachine = new SendStateMachine();
  private final ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine();
  private final ReceivedDataBuffer receivedDataBuffer = new ReceivedDataBuffer();

  public DefaultStream(
      final StreamId id,
      final FrameSender sender,
      final StreamListener listener,
      StreamType streamType) {
    this.id = id;
    this.sender = sender;
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
    FullPacket p = sender.send(sf);

    sendStateMachine.onStream(p.getPacketNumber(), finish);
  }

  public void reset(int applicationErrorCode) {
    canReset();

    final Frame frame = new ResetStreamFrame(id, applicationErrorCode, offset.get());

    final FullPacket p = sender.send(frame);

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

    receiveStateMachine.onStream(finish);

    // TODO review use of state machine, canReceive seems broken in casse of out of order packets
    if (!receiveStateMachine.canReceive() && receivedDataBuffer.isDone()) {
      listener.onFinished();
    }
  }

  public void onReset(final int applicationErrorCode, final long offset) {
    receiveStateMachine.onReset();
    listener.onReset(this, applicationErrorCode, offset);
    receiveStateMachine.onAppReadReset();
  }

  public void onAck(PacketNumber pn) {
    sendStateMachine.onAck(pn);
  }

  public boolean isFinished() {
    return !sendStateMachine.canSend() || !receiveStateMachine.canReceive();
  }
}
