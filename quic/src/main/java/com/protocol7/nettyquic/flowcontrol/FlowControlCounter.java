package com.protocol7.nettyquic.flowcontrol;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;

import com.protocol7.nettyquic.protocol.StreamId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FlowControlCounter {

  // TODO make sure max bytes does not grow forever

  private final AtomicLong connectionMaxBytes;
  private final long defaultStreamMaxBytes;

  private class StreamCounter {
    public boolean finished = false;
    public final AtomicLong maxOffset = new AtomicLong(defaultStreamMaxBytes);
    public final AtomicLong offset = new AtomicLong(0);
  }

  // TODO this will grow forever. Consider how we can garbage collect finished streams while not
  // recreating them on out-of-order packets
  private final Map<StreamId, StreamCounter> streams = new ConcurrentHashMap<>();

  public FlowControlCounter(final long connectionMaxBytes, final long streamMaxBytes) {
    this.connectionMaxBytes = new AtomicLong(connectionMaxBytes);
    this.defaultStreamMaxBytes = streamMaxBytes;
  }

  private long calculateConnectionOffset() {
    return streams.values().stream().mapToLong(c -> c.offset.get()).sum();
  }

  // remove need to syncronize
  public synchronized TryConsumeResult tryConsume(final StreamId sid, final long offset) {
    checkArgument(offset > 0);

    // first check if we can successfully consume
    final StreamCounter stream = streams.computeIfAbsent(sid, ignored -> new StreamCounter());
    final long streamMax = stream.maxOffset.get();
    final AtomicLong streamConsumed = stream.offset;
    final long connOffset = calculateConnectionOffset();

    final long streamDelta = offset - streamConsumed.get();

    final long resultingConnOffset;
    final long resultingStreamOffset;
    final boolean success;
    if (streamDelta < 0) {
      // out of order, always successful
      success = true;
      resultingConnOffset = connOffset;
      resultingStreamOffset = streamConsumed.get();
    } else if (streamDelta > 0 && stream.finished) {
      // trying to increase offset for finished stream, bail
      throw new IllegalStateException("Stream finished");
    } else if (offset > streamMax || connOffset + streamDelta > connectionMaxBytes.get()) {
      success = false;
      resultingConnOffset = connOffset + streamDelta;
      resultingStreamOffset = offset;
    } else {
      success = true;
      streamConsumed.updateAndGet(current -> max(current, offset));
      resultingConnOffset = connOffset + streamDelta;
      resultingStreamOffset = streamConsumed.get();
    }

    return new TryConsumeResult(
        success, resultingConnOffset, connectionMaxBytes.get(), resultingStreamOffset, streamMax);
  }

  public void resetStream(final StreamId sid, final long finalOffset) {
    final StreamCounter stream = streams.computeIfAbsent(sid, ignored -> new StreamCounter());
    stream.offset.updateAndGet(current -> max(current, finalOffset));
    stream.finished = true;
  }

  public void setConnectionMaxBytes(final long connectionMaxBytes) {
    checkArgument(connectionMaxBytes > 0);

    this.connectionMaxBytes.updateAndGet(current -> max(connectionMaxBytes, current));
  }

  public long increaseStreamMax(final StreamId sid) {
    final StreamCounter stream = streams.computeIfAbsent(sid, ignored -> new StreamCounter());
    final AtomicLong streamMax = stream.maxOffset;

    // double
    return streamMax.addAndGet(streamMax.get());
  }

  public long increaseConnectionMax() {
    // double
    return connectionMaxBytes.addAndGet(connectionMaxBytes.get());
  }

  public void setStreamMaxBytes(final StreamId sid, final long streamMaxBytes) {
    checkArgument(streamMaxBytes > 0);

    final StreamCounter stream = streams.computeIfAbsent(sid, ignored -> new StreamCounter());
    final AtomicLong streamMax = stream.maxOffset;

    streamMax.updateAndGet(current -> max(streamMaxBytes, current));
  }
}
