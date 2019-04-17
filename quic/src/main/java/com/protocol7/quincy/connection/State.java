package com.protocol7.quincy.connection;

public enum State {
  Started,
  BeforeHello,
  BeforeHandshake,
  BeforeReady,
  Ready,
  Closing,
  Closed
}
