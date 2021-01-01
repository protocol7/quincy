package com.protocol7.quincy.reliability;

import static com.protocol7.quincy.protocol.ConnectionId.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.MaxDataFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.utils.Ticker;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PacketBufferTest {

  @Mock private Ticker ticker;
  private long pn1 = 1;
  private long pn2 = 2;
  private FullPacket packet1 = p(pn1);
  private FullPacket packet2 = p(pn2);

  private PacketBuffer buffer;

  @Before
  public void setUp() {
    when(ticker.nanoTime()).thenReturn(0L);

    buffer = new PacketBuffer(ticker);
  }

  @Test
  public void test() {
    assertTrue(buffer.isEmpty());
    assertFalse(buffer.contains(pn1));

    buffer.put(packet1);

    assertFalse(buffer.isEmpty());
    assertTrue(buffer.contains(pn1));

    assertTrue(buffer.remove(pn1));

    assertTrue(buffer.isEmpty());
    assertFalse(buffer.contains(pn1));
  }

  @Test
  public void remove() {
    assertFalse(buffer.remove(pn1));
    buffer.put(packet1);
    assertTrue(buffer.remove(pn1));
  }

  @Test
  public void drain() {
    when(ticker.nanoTime()).thenReturn(100L);

    assertTrue(buffer.drainSince(10, TimeUnit.NANOSECONDS).isEmpty());

    buffer.put(packet1);
    when(ticker.nanoTime()).thenReturn(200L);
    buffer.put(packet2);

    assertEquals(List.of(f(1)), buffer.drainSince(10, TimeUnit.NANOSECONDS));
    assertTrue(buffer.drainSince(10, TimeUnit.NANOSECONDS).isEmpty());

    when(ticker.nanoTime()).thenReturn(300L);
    assertEquals(List.of(f(2)), buffer.drainSince(10, TimeUnit.NANOSECONDS));
    assertTrue(buffer.drainSince(10, TimeUnit.NANOSECONDS).isEmpty());
  }

  private FullPacket p(final long pn) {
    return ShortPacket.create(false, EMPTY, pn, f(pn));
  }

  private Frame f(final long i) {
    return new MaxDataFrame(i);
  }
}
