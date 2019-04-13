package com.protocol7.nettyquic.reliability;

import static java.util.Objects.requireNonNull;

import com.protocol7.nettyquic.utils.Ticker;
import java.util.concurrent.TimeUnit;

public class AckDelay {

  private final int ackDelayMultiplier;
  private final Ticker ticker;

  public AckDelay(final int ackDelayExponent, final Ticker ticker) {
    this.ackDelayMultiplier = (int) Math.pow(2, ackDelayExponent);
    this.ticker = requireNonNull(ticker);
  }

  public long calculate(long delay, TimeUnit unit) {
    return Math.max(unit.toMicros(delay) / ackDelayMultiplier, 0);
  }

  public long time() {
    return ticker.nanoTime();
  }

  public long delay(long delay) {
    return Math.max(ticker.nanoTime() - delay, 0);
  }
}
