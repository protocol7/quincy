package com.protocol7.nettyquic.flow;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class FlowController {

  private final AtomicLong bytesRead = new AtomicLong(0);
  private final AtomicLong bytesWritten = new AtomicLong(0);

  private final AtomicLong readMax;
  private final AtomicLong writeMax;
  private final Consumer<Long> readMaxUpdate;
  private final long readDelta;
  private final double readThreshold;
  private final ReentrantLock readUpdateLock = new ReentrantLock();

  public FlowController(
      final long readMax,
      final long writeMax,
      final Consumer<Long> readMaxUpdate,
      final long readDelta,
      final double readThreshold) {
    this.readMax = new AtomicLong(readMax);
    this.writeMax = new AtomicLong(writeMax);
    this.readMaxUpdate = readMaxUpdate;
    this.readDelta = readDelta;
    this.readThreshold = readThreshold;
  }

  public void addReadBytes(long read) {
    bytesRead.addAndGet(read);

    maybeUpdateReadMax();
  }

  private void maybeUpdateReadMax() {
      readUpdateLock.lock();
      try {
          long remainngRead = readMax.get() - bytesRead.get();
          if (remainngRead < readDelta * readThreshold) {
              long newMax = bytesRead.get() + readDelta;
              readMaxUpdate.accept(newMax);
          }
      } finally{
          readUpdateLock.unlock();
      }
  }

  public void addWriteBytes(long written) {
    bytesWritten.addAndGet(written);
  }

  public boolean canWrite(long len) {
    return remainingWrite() >= len;
  }

  public long remainingWrite() {
    return writeMax.get() - bytesWritten.get();
  }

  public void updateWriteMax(final long max) {
    writeMax.updateAndGet(
        current -> {
          if (max > current) {
            return max;
          } else {
            return current;
          }
        });
  }
}
