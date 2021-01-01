package com.protocol7.quincy.utils;

public interface Ticker {

  static Ticker systemTicker() {
    return new Ticker() {

      @Override
      public long milliTime() {
        return System.currentTimeMillis();
      }

      @Override
      public long nanoTime() {
        return System.nanoTime();
      }
    };
  }

  long milliTime();

  long nanoTime();
}
