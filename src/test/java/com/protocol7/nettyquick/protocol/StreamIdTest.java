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
    for (int i = 0; i<1000_000; i++) {
      StreamId v = StreamId.random();
      assertTrue(v.toString(), v.getValue() > 0 && v.getValue() < 4611686018427387903L);
    }
  }

  @Test
  public void roundtrip() {
    StreamId streamId = StreamId.random();

    ByteBuf bb = Unpooled.buffer();
    streamId.write(bb);

    StreamId parsed = StreamId.parse(bb);

    assertEquals(streamId, parsed);
  }

}