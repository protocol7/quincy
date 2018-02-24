package com.protocol7.nettyquick.streams;

import com.protocol7.nettyquick.streams.Stream;

public interface StreamListener {
  void onData(Stream stream, byte[] data);
}
