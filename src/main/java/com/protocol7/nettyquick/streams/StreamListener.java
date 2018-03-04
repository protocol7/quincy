package com.protocol7.nettyquick.streams;

public interface StreamListener {
  void onData(Stream stream, byte[] data);
  void onDone();
  void onReset(Stream stream, int applicationErrorCode, long offset);
}
