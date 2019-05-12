package com.protocol7.quincy.streams;

public interface StreamListener {
  void onData(Stream stream, byte[] data, boolean finished);
}
