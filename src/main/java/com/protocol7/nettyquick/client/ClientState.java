package com.protocol7.nettyquick.client;

public enum ClientState {
  BeforeInitial,
  WaitingForServerHello,
  WaitingForHandshake,
  Ready,
  Closing,
  Closed
}
