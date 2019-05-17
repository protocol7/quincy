package com.protocol7.quincy.streams;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.StreamId;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.ResetStreamFrame;
import com.protocol7.quincy.protocol.frames.StreamFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultStream implements Stream {

  private final long id;
  private final FrameSender sender;
  private final StreamListener listener;
  private final AtomicLong offset = new AtomicLong(0);
  private final StreamType streamType;
  private final SendStateMachine sendStateMachine = new SendStateMachine();
  private final ReceiveStateMachine receiveStateMachine = new ReceiveStateMachine();
  private final ReceivedDataBuffer receivedDataBuffer = new ReceivedDataBuffer();
  private final AtomicBoolean seenFinish = new AtomicBoolean(false);

  public DefaultStream(
      final long id,
      final FrameSender sender,
      final StreamListener listener,
      final StreamType streamType) {
    this.id = StreamId.validate(id);
    this.sender = sender;
    this.listener = listener;
    this.streamType = streamType;
  }

  public long getId() {
    return id;
  }

  public StreamType getStreamType() {
    return streamType;
  }

  public void write(final byte[] b, final boolean finish) {
    canWrite();

    final long frameOffset = offset.getAndAdd(b.length);
    final StreamFrame sf = new StreamFrame(id, frameOffset, finish, b);
    final FullPacket p = sender.send(sf);

    sendStateMachine.onStream(p.getPacketNumber(), finish);
  }

  public void reset(final int applicationErrorCode) {
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
    if (finish) {
      seenFinish.set(true);
    }

    receivedDataBuffer.onData(b, offset, finish);

    while (receivedDataBuffer.hasMore()) {
      final Optional<byte[]> data = receivedDataBuffer.read();

      listener.onData(this, data.get(), receivedDataBuffer.isDone() && seenFinish.get());
    }

    receiveStateMachine.onStream(finish);
  }

  public void onReset(final int applicationErrorCode, final long offset) {
    receiveStateMachine.onReset();
    receiveStateMachine.onAppReadReset();
  }

  public void onAck(final PacketNumber pn) {
    sendStateMachine.onAck(pn);
  }

  public boolean isFinished() {
    return !sendStateMachine.canSend() || !receiveStateMachine.canReceive();
  }
}
