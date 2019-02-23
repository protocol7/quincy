package com.protocol7.nettyquic.client;

public enum ClientState {
  BeforeInitial,
  WaitingForServerHello,
  WaitingForHandshake,
  Ready,
  Closing,
  Closed
}
