package com.protocol7.quincy.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class PacketNumberTest {

  @Test
  public void validateBounds() {
    PacketNumber.validate(0);
    PacketNumber.validate(123);
    PacketNumber.validate(4611686018427387903L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooSmall() {
    PacketNumber.validate(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooLarge() {
    PacketNumber.validate(4611686018427387904L);
  }

  @Test
  public void next() {
    final long pn = 123;

    assertEquals(124, PacketNumber.next(pn));
  }

  @Test
  public void roundtrip() {
    final long pn = 123;

    final byte[] b = PacketNumber.write(pn);

    final long parsed = PacketNumber.parse(b);

    assertEquals(pn, parsed);
  }

  @Test
  public void parse() {
    assertRead(0x0, "00");
    assertRead(0x19, "19");
    assertRead(1, "0001");
    assertRead(255, "FF");
    assertRead(256, "0100");
    assertRead(65535, "FFFF");
    assertRead(65536, "010000");
    assertRead(16777215, "FFFFFF");
    assertRead(4294967295L, "FFFFFFFF");
    assertRead(0x3719, "3719");
    assertRead(0x2589fa19, "2589fa19");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseEmpty() {
    PacketNumber.parse(new byte[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseTooLong() {
    PacketNumber.parse(new byte[5]);
  }

  @Test
  public void write() {
    assertWrite(0x0, "00");
    assertWrite(0xFF, "ff");
    assertWrite(0x100, "0100");
    assertWrite(0xFFFF, "ffff");
    assertWrite(0x10000, "010000");
    assertWrite(0xFFFFFF, "ffffff");
    assertWrite(0x1000000, "01000000");
    assertWrite(0xFFFFFFFFL, "ffffffff");

    assertWrite(0x19, "19");
    assertWrite(0x3719, "3719");
    assertWrite(0x2589fa19, "2589fa19");
    assertWrite(1160621137, "452dac51");
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeTooSmall() {
    PacketNumber.write(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void writeTooLarge() {
    PacketNumber.write(0x100000000L);
  }

  private void assertRead(final long expected, final String h) {
    final long pn = PacketNumber.parse(Hex.dehex(h));
    assertEquals(expected, pn);
  }

  private void assertWrite(final long pn, final String expected) {
    final ByteBuf bb = Unpooled.buffer();
    bb.writeBytes(PacketNumber.write(pn));

    final byte[] b = Bytes.drainToArray(bb);

    assertEquals(expected, Hex.hex(b));
  }
}
