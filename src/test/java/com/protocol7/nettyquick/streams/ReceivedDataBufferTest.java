package com.protocol7.nettyquick.streams;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReceivedDataBufferTest {

  public static final byte[] DATA1 = "hello".getBytes();
  public static final byte[] DATA2 = "world".getBytes();

  private final ReceivedDataBuffer buffer = new ReceivedDataBuffer();

  @Test
  public void inOrder() {
    buffer.onData(DATA1, 0, false);
    assertFalse(buffer.isDone());
    assertArrayEquals(DATA1, buffer.read().get());

    buffer.onData(DATA2, DATA1.length, true);

    assertArrayEquals(DATA2, buffer.read().get());
    assertTrue(buffer.isDone());
  }

  @Test
  public void outOfOrder() {
    buffer.onData(DATA2, DATA1.length, true);

    assertFalse(buffer.isDone());
    assertFalse(buffer.read().isPresent());

    buffer.onData(DATA1, 0, false);

    assertArrayEquals(DATA1, buffer.read().get());
    assertArrayEquals(DATA2, buffer.read().get());
    assertTrue(buffer.isDone());
  }

}