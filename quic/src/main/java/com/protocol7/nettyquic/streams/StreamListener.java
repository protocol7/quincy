package com.protocol7.nettyquic.streams;

public interface StreamListener {

  void onData(Stream stream, byte[] data);

  void onFinished();

  void onReset(Stream stream, int applicationErrorCode, long offset);
}
