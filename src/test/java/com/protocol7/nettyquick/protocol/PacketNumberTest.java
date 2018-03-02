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
      assertTrue(v.toString(), v.asLong() > 0 && v.asLong() < 4611686018427387903L);
    }
  }

  @Test
  public void write() {
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(123).write(bb);
    TestUtil.assertBuffer("000000000000007b", bb);
  }

  @Test
  public void writeAsVarint() {
    ByteBuf bb = Unpooled.buffer();
    new PacketNumber(123).writeVarint(bb);
    Bytes.debug(bb);
    TestUtil.assertBuffer("407b", bb);
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
  public void read4() {
    PacketNumber lastAcked = new PacketNumber(2860708622L);

    ByteBuf bb = Unpooled.copiedBuffer(Hex.dehex("12341f94"));
    PacketNumber pn = PacketNumber.read4(bb, lastAcked);
    assertEquals(new PacketNumber(4600373140L), pn);
  }
}