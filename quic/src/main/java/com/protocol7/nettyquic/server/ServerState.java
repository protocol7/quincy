package com.protocol7.nettyquic.server;

public enum ServerState {
  BeforeInitial,
  WaitingForFinished,
  Ready,
  Closing,
  Closed
}
