package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquick.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class VarintTest {

  @Test
  public void randomBounds() {
    for (int i = 0; i<1000_000; i++) {
      Varint v = Varint.random();
      assertTrue(v.toString(), v.getValue() > 0 && v.getValue() < 4611686018427387903L);
    }
  }

  @Test
  public void parse8() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"));

    Varint vi = Varint.read(bb);

    assertEquals(151288809941952652L, vi.getValue());
    assertFalse(bb.isReadable());
  }

  @Test
  public void parse4() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("9d 7f 3e 7d"));

    Varint vi = Varint.read(bb);

    assertEquals(494878333, vi.getValue());
    assertFalse(bb.isReadable());
  }

  @Test
  public void parse2() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("7b bd"));

    Varint vi = Varint.read(bb);

    assertEquals(15293, vi.getValue());
    assertFalse(bb.isReadable());
  }

  @Test
  public void parse1() {
    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("25"));

    Varint vi = Varint.read(bb);

    assertEquals(37, vi.getValue());
    assertFalse(bb.isReadable());
  }

  @Test
  public void write8() {
    assertWrite(Hex.dehex("c2 19 7c 5e ff 14 e8 8c"), new Varint(151288809941952652L), 8);
  }

  @Test
  public void write4() {
    assertWrite(Hex.dehex("9d 7f 3e 7d"), new Varint(494878333), 4);
  }

  @Test
  public void write2() {
    assertWrite(Hex.dehex("7b bd"), new Varint(15293), 2);
  }

  @Test
  public void write1() {
    assertWrite(Hex.dehex("25"), new Varint(37), 1);
  }

  private void assertWrite(byte[] expected, Varint vi, int len) {
    ByteBuf bb = Unpooled.buffer();
    vi.write(bb);

    byte[] b = new byte[len];
    bb.readBytes(b);

    assertArrayEquals(expected, b);
    assertFalse(bb.isReadable());
  }
}