package com.protocol7.nettyquic.flowcontrol;

import com.protocol7.nettyquic.protocol.StreamId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.max;

public class FlowControlCounter {

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

  public static class TryConsumeResult {
    private final boolean success;
    private final double connection;
    private final double stream;

    public TryConsumeResult(final boolean success, final double connection, final double stream) {
      this.success = success;
      this.connection = connection;
      this.stream = stream;
    }

    public boolean isSuccessful() {
      return success;
    }

    public double getConnection() {
      return connection;
    }

    public double getStream() {
      return stream;
    }
  }

  private long calculateConnectionOffset() {
    return streams.values().stream().mapToLong(c -> c.offset.get()).sum();
  }

  // remove need to syncronize
  public synchronized TryConsumeResult tryConsume(StreamId sid, long offset) {
    checkArgument(offset > 0);

    // first check if we can successfully consume
    final StreamCounter stream = streams.computeIfAbsent(sid, ignored -> new StreamCounter());
    final long streamMax = stream.maxOffset.get();
    final AtomicLong streamConsumed = stream.offset;
    final long connOffset = calculateConnectionOffset();

    final long streamDelta = offset - streamConsumed.get();

    long resultingConnOffset;
    boolean success;
    if (streamDelta < 0) {
      // out of order, always successful
      success = true;
      resultingConnOffset = connOffset;
    } else if (streamDelta > 0 && stream.finished) {
      // trying to increase offset for finished stream, bail
      throw new IllegalStateException("Stream finished");
    } else if (offset > streamMax || connOffset + streamDelta > connectionMaxBytes.get()) {
      success = false;
      resultingConnOffset = connOffset;
    } else {
      success = true;
      streamConsumed.updateAndGet(current -> max(current, offset));
      resultingConnOffset = connOffset + streamDelta;
    }

    return new TryConsumeResult(
        success,
        1.0 * resultingConnOffset / connectionMaxBytes.get(),
        1.0 * streamConsumed.get() / streamMax);
  }

  public void finishStream(StreamId sid, long finalOffset) {
    final StreamCounter stream = streams.computeIfAbsent(sid, ignored -> new StreamCounter());
    stream.offset.updateAndGet(current -> max(current, finalOffset));
    stream.finished = true;
  }

  public void setConnectionMaxBytes(long connectionMaxBytes) {
    checkArgument(connectionMaxBytes > 0);

    this.connectionMaxBytes.updateAndGet(current -> max(connectionMaxBytes, current));
  }

  public void setStreamMaxBytes(StreamId sid, long streamMaxBytes) {
    checkArgument(streamMaxBytes > 0);

    final StreamCounter stream = streams.computeIfAbsent(sid, ignored -> new StreamCounter());
    final AtomicLong streamMax = stream.maxOffset;

    streamMax.updateAndGet(current -> max(streamMaxBytes, current));
  }
}
