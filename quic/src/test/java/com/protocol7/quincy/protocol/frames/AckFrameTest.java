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
    final List<AckBlock> blocks =
        List.of(new AckBlock(1, 5), new AckBlock(7, 8), new AckBlock(12, 100));
    final AckFrame frame = new AckFrame(1234, blocks);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final AckFrame parsed = AckFrame.parse(bb);

    assertEquals(frame.getAckDelay(), parsed.getAckDelay());
    assertEquals(frame.getBlocks(), parsed.getBlocks());
  }

  @Test
  public void roundtripSinglePacket() {
    final List<AckBlock> blocks = List.of(new AckBlock(100, 100));
    final AckFrame frame = new AckFrame(1234, blocks);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    final AckFrame parsed = AckFrame.parse(bb);

    assertEquals(frame.getAckDelay(), parsed.getAckDelay());
    assertEquals(frame.getBlocks(), parsed.getBlocks());
  }

  @Test
  public void writeSinglePacket() {
    final List<AckBlock> blocks = List.of(new AckBlock(100, 100));
    final AckFrame frame = new AckFrame(1234, blocks);

    final ByteBuf bb = Unpooled.buffer();
    frame.write(bb);

    assertEquals("02406444d20000", Hex.hex(Bytes.drainToArray(bb)));
  }
}
