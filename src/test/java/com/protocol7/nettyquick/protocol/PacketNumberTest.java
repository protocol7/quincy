package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.protocol7.nettyquick.TestUtil;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Hex;
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
  public void randomBounds() {
    for (int i = 0; i < 1000_000; i++) {
      PacketNumber v = PacketNumber.random();
      assertTrue(v.toString(), v.asLong() > 0 && v.asLong() < 4294966271L);
    }
  }

  @Test
  public void write() {
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(123).write(bb);
    TestUtil.assertBuffer("807b", bb);
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
    ByteBuf bb = Unpooled.buffer();

    pn.write(bb);

    PacketNumber parsed = PacketNumber.parseVarint(bb);

    assertEquals(pn, parsed);
  }

  @Test
  public void parseVarint() {
    assertRead(0x19, "19");
    assertRead(1, "8001");
    assertRead(0x3719, "b719");
    assertRead(0x2589fa19, "e589fa19");
  }

  @Test
  public void writeVarint() {
    assertWrite(0x19, "19");
    assertWrite(0x3719, "b719");
    assertWrite(0x2589fa19, "e589fa19");
    assertWrite(1160621137, "c52dac51");
  }

  @Test
  public void roundtripVarint() {
    int pn = (int) PacketNumber.random().asLong();
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(pn).write(bb);

    PacketNumber parsed = PacketNumber.parseVarint(bb);

    assertEquals(pn, parsed.asLong());
  }

  private void assertRead(int expected, String h) {
    PacketNumber pn = PacketNumber.parseVarint(bb(h));
    assertEquals(expected, pn.asLong());
  }

  private void assertWrite(int pn, String expected) {
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(pn).write(bb);

    byte[] b = Bytes.drainToArray(bb);

    assertEquals(expected, Hex.hex(b));
  }

  private ByteBuf bb(String h) {
    return Unpooled.wrappedBuffer(Hex.dehex(h));
  }
}
