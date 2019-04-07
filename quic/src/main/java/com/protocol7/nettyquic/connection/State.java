package com.protocol7.nettyquic.connection;

public enum State {
  Started,
  BeforeHello,
  BeforeHandshake,
  BeforeReady,
  Ready,
  Closing,
  Closed
}
