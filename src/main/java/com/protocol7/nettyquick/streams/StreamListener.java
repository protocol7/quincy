package com.protocol7.nettyquick.streams;

public interface StreamListener {
  void onData(Stream stream, long offset, byte[] data);
}
