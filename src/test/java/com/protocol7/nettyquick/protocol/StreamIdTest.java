package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamIdTest {

  @Test
  public void validateBounds() {
    new StreamId(0);
    new StreamId(123);
    new StreamId(4611686018427387903L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooSmall() {
    new StreamId(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooLarge() {
    new StreamId(4611686018427387904L);
  }

  @Test
  public void randomBounds() {
    for (int i = 0; i < 1000_000; i++) {
      StreamId v = StreamId.random(true, true);
      assertTrue(v.toString(), v.getValue() > 1 && v.getValue() < 4611686018427387903L);
    }
  }

  @Test
  public void randomClient() {
    for (int i = 0; i < 1000; i++) {
      StreamId id = StreamId.random(true, true);
      assertTrue((id.getValue() & 1) == 0);
      assertTrue(id.isClient());
    }
  }

  @Test
  public void randomServer() {
    for (int i = 0; i < 1000; i++) {
      StreamId id = StreamId.random(false, true);
      assertTrue((id.getValue() & 1) == 1);
      assertFalse(id.isClient());
    }
  }

  @Test
  public void randomBidirectional() {
    for (int i = 0; i < 1000; i++) {
      StreamId id = StreamId.random(true, true);
      assertTrue((id.getValue() & 0b10) == 0);
      assertTrue(id.isBidirectional());
    }
  }

  @Test
  public void randomUnidirectional() {
    for (int i = 0; i < 1000; i++) {
      StreamId id = StreamId.random(true, false);
      assertTrue((id.getValue() & 0b10) == 0b10);
      assertFalse(id.isBidirectional());
    }
  }

  @Test
  public void next() {
    assertEquals(new StreamId(3), StreamId.next(new StreamId(0), false, false));
    assertEquals(new StreamId(1), StreamId.next(new StreamId(0), false, true));
    assertEquals(new StreamId(4), StreamId.next(new StreamId(0), true, true));
  }

  @Test
  public void roundtrip() {
    StreamId streamId = StreamId.random(true, true);

    ByteBuf bb = Unpooled.buffer();
    streamId.write(bb);

    StreamId parsed = StreamId.parse(bb);

    assertEquals(streamId, parsed);
  }
}
