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
      StreamId v = StreamId.random(true, true);
      assertTrue(v.toString(), v.getValue() > 0 && v.getValue() < 4611686018427387903L);
    }
  }

  @Test
  public void randomClient() {
    for (int i = 0; i<1000; i++) {
      long v = StreamId.random(true, true).getValue();
      assertTrue((v & 1) == 0);
    }
  }

  @Test
  public void randomServer() {
    for (int i = 0; i<1000; i++) {
      long v = StreamId.random(false, true).getValue();
      assertTrue((v & 1) == 1);
    }
  }

  @Test
  public void randomBidirectional() {
    for (int i = 0; i<1000; i++) {
      long v = StreamId.random(true, true).getValue();
      assertTrue((v & 0b10) == 0);
    }
  }

  @Test
  public void randomUnidirectional() {
    for (int i = 0; i<1000; i++) {
      long v = StreamId.random(true, false).getValue();
      assertTrue((v & 0b10) == 0b10);
    }
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