package com.protocol7.quincy.connection;

public enum State {
  Started,
  BeforeHello,
  BeforeHandshake,
  BeforeDone,
  Done,
  Closing,
  Closed
}
