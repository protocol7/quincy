package com.protocol7.nettyquic.client;

import com.protocol7.nettyquic.connection.State;

public enum ClientState implements State {
  BeforeInitial,
  WaitingForServerHello,
  WaitingForHandshake,
  Ready,
  Closing,
  Closed
}
