package com.protocol7.quincy.streams;

import java.util.Optional;
import java.util.TreeMap;

// TODO optimize
public class ReceivedDataBuffer {

  private final TreeMap<Long, byte[]> buffer = new TreeMap<>();
  private long largestOffset = 0;
  private long readOffset = 0;

  public void onData(final byte[] data, final long offset, final boolean finish) {
    buffer.put(offset, data);
    if (finish) {
      this.largestOffset = offset;
    }
  }

  public Optional<byte[]> read() {
    final byte[] b = buffer.get(readOffset);

    if (b != null) {
      readOffset += b.length;
      return Optional.of(b);
    } else {
      return Optional.empty();
    }
  }

  public boolean isDone() {
    return readOffset > largestOffset;
  }
}
