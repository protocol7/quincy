package com.protocol7.quincy.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.TestUtil;
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
  public void write() {
    final byte[] b = PacketNumber.write(123, 4);
    TestUtil.assertHex("0000007b", b);
  }

  @Test
  public void next() {
    final long pn = 123;

    assertEquals(124, PacketNumber.next(pn));
  }

  @Test
  public void roundtrip() {
    final long pn = 123;

    final byte[] b = PacketNumber.write(pn, PacketNumber.getLength(pn));

    final long parsed = PacketNumber.parse(b);

    assertEquals(pn, parsed);
  }

  @Test
  public void parseVarint() {
    assertRead(0x19, "19");
    assertRead(1, "0001");
    assertRead(0x3719, "3719");
    assertRead(0x2589fa19, "2589fa19");
  }

  @Test
  public void writeVarint() {
    assertWrite(0x19, "00000019");
    assertWrite(0x3719, "00003719");
    assertWrite(0x2589fa19, "2589fa19");
    assertWrite(1160621137, "452dac51");
  }

  private void assertRead(final int expected, final String h) {
    final long pn = PacketNumber.parse(Hex.dehex(h));
    assertEquals(expected, pn);
  }

  private void assertWrite(final int pn, final String expected) {
    final ByteBuf bb = Unpooled.buffer();
    bb.writeBytes(PacketNumber.write(pn, PacketNumber.getLength(pn)));

    final byte[] b = Bytes.drainToArray(bb);

    assertEquals(expected, Hex.hex(b));
  }

  private ByteBuf bb(final String h) {
    return Unpooled.wrappedBuffer(Hex.dehex(h));
  }
}
