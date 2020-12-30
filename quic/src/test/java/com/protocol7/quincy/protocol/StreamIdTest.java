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
  public void next() {
    assertEquals(0, StreamId.next(-1, true, true));
    assertEquals(1, StreamId.next(-1, false, true));
    assertEquals(2, StreamId.next(-1, true, false));
    assertEquals(3, StreamId.next(-1, false, false));

    assertEquals(4, StreamId.next(0, true, true));
    assertEquals(5, StreamId.next(1, false, true));
    assertEquals(6, StreamId.next(2, true, false));
    assertEquals(7, StreamId.next(3, false, false));
  }

  @Test
  public void roundtrip() {
    final long streamId = StreamId.next(0, true, true);

    final ByteBuf bb = Unpooled.buffer();
    StreamId.write(bb, streamId);

    final long parsed = StreamId.parse(bb);

    assertEquals(streamId, parsed);
  }
}
