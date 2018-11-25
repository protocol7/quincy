package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.*;

import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

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
    for (int i = 0; i < 1000_000; i++) {
      long v = Varint.random();
      assertTrue(v > 0 && v < 4611686018427387903L);
    }
  }

  @Test
  public void read8() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"));

    long vi = Varint.readAsLong(bb);

    assertEquals(151288809941952652L, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void read8Bytes() {
    long vi = Varint.readAsLong(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"));

    assertEquals(151288809941952652L, vi);
  }

  @Test
  public void read4() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("9d 7f 3e 7d"));

    long vi = Varint.readAsLong(bb);

    assertEquals(494878333, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void read4Bytes() {
    long vi = Varint.readAsLong(Hex.dehex("9d 7f 3e 7d"));

    assertEquals(494878333, vi);
  }

  @Test
  public void read2() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("7b bd"));

    long vi = Varint.readAsLong(bb);

    assertEquals(15293, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void read2Bytes() {
    long vi = Varint.readAsLong(Hex.dehex("7b bd"));

    assertEquals(15293, vi);
  }

  @Test
  public void read1() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("25"));

    long vi = Varint.readAsLong(bb);

    assertEquals(37, vi);
    assertFalse(bb.isReadable());
  }

  @Test
  public void read1Bytes() {
    long vi = Varint.readAsLong(Hex.dehex("25"));

    assertEquals(37, vi);
  }

  @Test
  public void write8() {
    assertWrite(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"), 151288809941952652L);
  }

  @Test
  public void write8Bytes() {
    assertWriteBytes(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"), 151288809941952652L);
  }

  @Test
  public void write4() {
    assertWrite(Hex.dehex("9d 7f 3e 7d"), 494878333);
  }

  @Test
  public void write4Bytes() {
    assertWriteBytes(Hex.dehex("9d 7f 3e 7d"), 494878333);
  }

  @Test
  public void write2() {
    assertWrite(Hex.dehex("7b bd"), 15293);
  }

  @Test
  public void write2Bytes() {
    assertWriteBytes(Hex.dehex("7b bd"), 15293);
  }

  @Test
  public void write1() {
    assertWrite(Hex.dehex("25"), 37);
  }

  @Test
  public void write1Bytes() {
    assertWriteBytes(Hex.dehex("25"), 37);
  }

  private void assertWrite(byte[] expected, long vi) {
    ByteBuf bb = Unpooled.buffer();
    Varint.write(vi, bb);

    byte[] b = Bytes.drainToArray(bb);

    assertArrayEquals(expected, b);
    assertFalse(bb.isReadable());
  }

  private void assertWriteBytes(byte[] expected, long vi) {
    byte[] actual = Varint.write(vi);
    assertArrayEquals(expected, actual);
  }
}
