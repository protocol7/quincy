package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.*;

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
    for (int i = 0; i<1000_000; i++) {
      PacketNumber v = PacketNumber.random();
      assertTrue(v.toString(), v.asLong() > 0 && v.asLong() < 4294966271L);
    }
  }

  @Test
  public void write() {
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(123).write(bb);
    TestUtil.assertBuffer("000000000000007b", bb);
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

    PacketNumber parsed = PacketNumber.read(bb);

    assertEquals(pn, parsed);
  }

  @Test
  public void read1() {
    PacketNumber lastAcked = new PacketNumber(2860708622L);

    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("94"));
    PacketNumber pn = PacketNumber.read1(bb, lastAcked);
    assertEquals(new PacketNumber(2860708756L), pn);
  }

  @Test
  public void read2() {
    PacketNumber lastAcked = new PacketNumber(2860708622L);

    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("1f94"));
    PacketNumber pn = PacketNumber.read2(bb, lastAcked);
    assertEquals(new PacketNumber(2860720020L), pn);
  }

  @Test
  public void read2Overflow() {
    PacketNumber lastAcked = new PacketNumber(2868900622L);

    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("1f94"));
    PacketNumber pn = PacketNumber.read2(bb, lastAcked);
    assertTrue(pn.asLong() > 2868900622L);
    assertEquals(new PacketNumber(2885623700L), pn);
  }

  @Test
  public void read4() {
    PacketNumber lastAcked = new PacketNumber(2860708622L);

    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("12341f94"));
    PacketNumber pn = PacketNumber.read4(bb, lastAcked);
    assertEquals(new PacketNumber(4600373140L), pn);
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
    int pn = (int)PacketNumber.random().asLong();
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(pn).writeVarint(bb);

    PacketNumber parsed = PacketNumber.parseVarint(bb);

    assertEquals(pn, parsed.asLong());
  }

  private void assertRead(int expected, String h) {
    PacketNumber pn = PacketNumber.parseVarint(bb(h));
    assertEquals(expected, pn.asLong());
  }

  private void assertWrite(int pn, String expected) {
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(pn).writeVarint(bb);

    byte[] b = Bytes.asArray(bb);

    assertEquals(expected, Hex.hex(b));
  }

  private ByteBuf bb(String h) {
    return Unpooled.wrappedBuffer(Hex.dehex(h));
  }

}