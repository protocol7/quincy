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
    new PacketNumber(0);
    new PacketNumber(123);
    new PacketNumber(4611686018427387903L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooSmall() {
    new PacketNumber(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void validateBoundsTooLarge() {
    new PacketNumber(4611686018427387904L);
  }

  @Test
  public void write() {
    byte[] b = new PacketNumber(123).write(4);
    TestUtil.assertHex("0000007b", b);
  }

  @Test
  public void next() {
    PacketNumber pn = new PacketNumber(123);

    assertEquals(new PacketNumber(124), pn.next());
    assertEquals(new PacketNumber(124), pn.next()); // doesn't mutate the original value
  }

  @Test
  public void roundtrip() {
    PacketNumber pn = new PacketNumber(123);

    byte[] b = pn.write(pn.getLength());

    PacketNumber parsed = PacketNumber.parse(b);

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

  private void assertRead(int expected, String h) {
    PacketNumber pn = PacketNumber.parse(Hex.dehex(h));
    assertEquals(expected, pn.asLong());
  }

  private void assertWrite(int pn, String expected) {
    ByteBuf bb = Unpooled.buffer();
    PacketNumber p = new PacketNumber(pn);
    bb.writeBytes(p.write(p.getLength()));

    byte[] b = Bytes.drainToArray(bb);

    assertEquals(expected, Hex.hex(b));
  }

  private ByteBuf bb(String h) {
    return Unpooled.wrappedBuffer(Hex.dehex(h));
  }
}
