package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import static org.junit.Assert.*;

public class VarintTest {

  @Test
  public void validateBounds() {
    ByteBuf bb = Unpooled.buffer();
    Varint.write(0, bb);
    Varint.write(123, bb);
    Varint.write(4611686018427387903L, bb);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooSmall() {
    ByteBuf bb = Unpooled.buffer();
    Varint.write(-1, bb);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooLarge() {
    ByteBuf bb = Unpooled.buffer();
    Varint.write(4611686018427387904L, bb);
  }

  @Test
  public void randomBounds() {
    for (int i = 0; i<1000_000; i++) {
      long v = Varint.random();
      assertTrue( v > 0 && v < 4611686018427387903L);
    }
  }

  @Test
  public void parse8() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"));

    long vi = Varint.readAsLong(bb);

    assertEquals(151288809941952652L, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void parse4() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("9d 7f 3e 7d"));

    long vi = Varint.readAsLong(bb);

    assertEquals(494878333, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void parse2() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("7b bd"));

    long vi = Varint.readAsLong(bb);

    assertEquals(15293, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void parse1() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("25"));

    long vi = Varint.readAsLong(bb);

    assertEquals(37, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void write8() {
    assertWrite(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"), 151288809941952652L, 8);
  }

  @Test
  public void write4() {
    assertWrite(Hex.dehex("9d 7f 3e 7d"), 494878333, 4);
  }

  @Test
  public void write2() {
    assertWrite(Hex.dehex("7b bd"), 15293, 2);
  }

  @Test
  public void write1() {
    assertWrite(Hex.dehex("25"), 37, 1);
  }

  private void assertWrite(byte[] expected, long vi, int len) {
    ByteBuf bb = Unpooled.buffer();
    Varint.write(vi, bb);

    byte[] b = new byte[len];
    bb.readBytes(b);

    assertArrayEquals(expected, b);
    assertFalse(bb.isReadable());
  }
}