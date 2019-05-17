package com.protocol7.quincy.protocol;

import static org.junit.Assert.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class StreamIdTest {

  @Test
  public void validateBounds() {
    StreamId.validate(0);
    StreamId.validate(123);
    StreamId.validate(4611686018427387903L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooSmall() {
    StreamId.validate(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooLarge() {
    StreamId.validate(4611686018427387904L);
  }

  @Test
  public void randomBounds() {
    for (int i = 0; i < 1000_000; i++) {
      final long v = StreamId.random(true, true);
      assertTrue(Long.toString(v), v > 1 && v < 4611686018427387903L);
    }
  }

  @Test
  public void randomClient() {
    for (int i = 0; i < 1000; i++) {
      final long id = StreamId.random(true, true);
      assertTrue((id & 1) == 0);
      assertTrue(StreamId.isClient(id));
    }
  }

  @Test
  public void randomServer() {
    for (int i = 0; i < 1000; i++) {
      final long id = StreamId.random(false, true);
      assertTrue((id & 1) == 1);
      assertFalse(StreamId.isClient(id));
    }
  }

  @Test
  public void randomBidirectional() {
    for (int i = 0; i < 1000; i++) {
      final long id = StreamId.random(true, true);
      assertTrue((id & 0b10) == 0);
      assertTrue(StreamId.isBidirectional(id));
    }
  }

  @Test
  public void randomUnidirectional() {
    for (int i = 0; i < 1000; i++) {
      final long id = StreamId.random(true, false);
      assertTrue((id & 0b10) == 0b10);
      assertFalse(StreamId.isBidirectional(id));
    }
  }

  @Test
  public void next() {
    assertEquals(3, StreamId.next(0, false, false));
    assertEquals(1, StreamId.next(0, false, true));
    assertEquals(4, StreamId.next(0, true, true));
  }

  @Test
  public void roundtrip() {
    final long streamId = StreamId.random(true, true);

    final ByteBuf bb = Unpooled.buffer();
    StreamId.write(bb, streamId);

    final long parsed = StreamId.parse(bb);

    assertEquals(streamId, parsed);
  }
}
