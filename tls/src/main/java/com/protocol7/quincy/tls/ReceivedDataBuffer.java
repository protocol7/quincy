package com.protocol7.quincy.tls;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.TreeMap;

// TODO optimize
public class ReceivedDataBuffer {

  private final TreeMap<Long, byte[]> buffer = new TreeMap<>();

  public void onData(final byte[] data, final long offset) {
    buffer.put(offset, data);
  }

  public Optional<ByteBuf> read() {
    long readOffset = 0;
    final ByteBuf bb = Unpooled.buffer();

    while (buffer.containsKey(readOffset)) {
      final byte[] b = buffer.get(readOffset);
      bb.writeBytes(b);

      readOffset += b.length;
    }

    if (bb.writerIndex() > 0) {
      return Optional.of(bb);
    } else {
      return Optional.empty();
    }
  }
}
