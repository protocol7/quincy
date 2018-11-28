package com.protocol7.nettyquick.server;

public enum ServerState {
  BeforeInitial,
  WaitingForFinished,
  Ready,
  Closing,
  Closed
}
