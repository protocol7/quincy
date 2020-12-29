package com.protocol7.quincy.protocol.frames;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.Test;

public class AckFrameTest {

  @Test
  public void roundtrip() {
    final List<AckRange> ranges =
        List.of(new AckRange(1, 5), new AckRange(7, 8), new AckRange(12, 100));
    final AckFrame frame = new AckFrame(1234, ranges);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final AckFrame parsed = AckFrame.parse(bb);

    assertEquals(frame.getAckDelay(), parsed.getAckDelay());
    assertEquals(frame.getRanges(), parsed.getRanges());
  }

  @Test
  public void roundtripSinglePacket() {
    final List<AckRange> ranges = List.of(new AckRange(100, 100));
    final AckFrame frame = new AckFrame(1234, ranges);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final AckFrame parsed = AckFrame.parse(bb);

    assertEquals(frame.getAckDelay(), parsed.getAckDelay());
    assertEquals(frame.getRanges(), parsed.getRanges());
  }

  @Test
  public void writeSinglePacket() {
    final List<AckRange> ranges = List.of(new AckRange(100, 100));
    final AckFrame frame = new AckFrame(1234, ranges);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals("02406444d20000", Hex.hex(Bytes.drainToArray(bb)));
  }
}
