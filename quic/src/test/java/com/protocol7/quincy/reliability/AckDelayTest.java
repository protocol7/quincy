package com.protocol7.quincy.reliability;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.utils.Ticker;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AckDelayTest {

  @Mock private Ticker ticker;

  private AckDelay ackDelay;

  @Before
  public void setUp() {
    when(ticker.nanoTime()).thenReturn(123L);
    ackDelay = new AckDelay(3, ticker);
  }

  @Test
  public void toAckDelay() {
    assertEquals(100, ackDelay.calculate(800 * 1000, TimeUnit.NANOSECONDS));
  }

  @Test
  public void toAckDelayNegative() {
    assertEquals(0, ackDelay.calculate(-800 * 1000, TimeUnit.NANOSECONDS));
  }

  @Test
  public void time() {
    assertEquals(123, ackDelay.time());
  }

  @Test
  public void delay() {
    assertEquals(23, ackDelay.delay(100));
  }

  @Test
  public void delayNegative() {
    assertEquals(0, ackDelay.delay(200));
  }
}
