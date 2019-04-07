package com.protocol7.nettyquic.utils;

public interface Ticker {

  static Ticker systemTicker() {
    return System::nanoTime;
  }

  long nanoTime();
}
