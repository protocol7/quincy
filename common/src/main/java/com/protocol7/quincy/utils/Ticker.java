package com.protocol7.quincy.utils;

public interface Ticker {

  static Ticker systemTicker() {
    return System::nanoTime;
  }

  long nanoTime();
}
