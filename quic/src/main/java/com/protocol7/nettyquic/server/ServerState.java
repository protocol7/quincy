package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.connection.State;

public enum ServerState implements State {
  BeforeInitial,
  WaitingForFinished,
  Ready,
  Closing,
  Closed
}
