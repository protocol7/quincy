package com.protocol7.quincy.streams;

import com.protocol7.quincy.protocol.StreamId;

public interface Stream {

  StreamId getId();

  StreamType getStreamType();

  void write(final byte[] b, boolean finish);

  void reset(int applicationErrorCode);

  boolean isFinished();
}
