package com.protocol7.nettyquick.server;

public interface StreamHandler {

  void onData(ServerStream stream, byte[] data);
}
