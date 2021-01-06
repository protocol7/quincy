package com.protocol7.quincy.streams;

public interface StreamHandler {
  void onData(Stream stream, byte[] data, boolean finished);
}
