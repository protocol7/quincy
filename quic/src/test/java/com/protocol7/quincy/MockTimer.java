package com.protocol7.quincy;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MockTimer implements Timer {

  public Set<Timeout> timeouts = new HashSet<>();

  @Override
  public Timeout newTimeout(final TimerTask timerTask, final long l, final TimeUnit timeUnit) {
    final Timeout timeout =
        new Timeout() {
          @Override
          public Timer timer() {
            return MockTimer.this;
          }

          @Override
          public TimerTask task() {
            return timerTask;
          }

          @Override
          public boolean isExpired() {
            return false;
          }

          @Override
          public boolean isCancelled() {
            return false;
          }

          @Override
          public boolean cancel() {
            return false;
          }
        };
    timeouts.add(timeout);
    return timeout;
  }

  @Override
  public Set<Timeout> stop() {
    return timeouts;
  }

  public void trigger() throws Exception {
    for (Timeout timeout : timeouts) {
      timeout.task().run(timeout);
    }
  }
}
