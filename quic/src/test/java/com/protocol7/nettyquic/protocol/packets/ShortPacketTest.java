package com.protocol7.nettyquic.protocol.packets;

import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Payload;
import com.protocol7.nettyquic.protocol.frames.PingFrame;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.TestAEAD;
import com.protocol7.nettyquic.utils.Bits;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class ShortPacketTest {

  private ConnectionId dest = ConnectionId.random();
  private PacketNumber pn = new PacketNumber(650000);

  private AEAD aead = TestAEAD.create();

  @Test
  public void roundtrip() {
    ShortPacket packet = packet();
    ByteBuf bb = buffer(packet);

    ShortPacket parsed = ShortPacket.parse(bb, dest.getLength()).complete(level -> aead);

    assertEquals(packet.getDestinationConnectionId(), parsed.getDestinationConnectionId());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getPayload(), parsed.getPayload());
  }

  @Test
  public void roundtripPrefix() {
    // make sure packet doesn't need to start at 0 in buffer

    ShortPacket packet = packet();
    ByteBuf bb = Unpooled.buffer();

    byte[] b = new byte[123];
    bb.writeBytes(b);

    packet.write(bb, aead);

    bb.readBytes(b);

    ShortPacket parsed = ShortPacket.parse(bb, dest.getLength()).complete(level -> aead);

    assertEquals(packet.getDestinationConnectionId(), parsed.getDestinationConnectionId());
    assertEquals(packet.getPacketNumber(), parsed.getPacketNumber());
    assertEquals(packet.getPayload(), parsed.getPayload());
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidFirstBit() {
    ByteBuf bb = buffer(packet());
    // switch on first bit, must make this payload invalid
    bb.setByte(0, Bits.set(bb.getByte(0), 7));
    ShortPacket.parse(bb, dest.getLength());
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidSecondBit() {
    ByteBuf bb = buffer(packet());
    // switch off second bit, must make this payload invalid
    bb.setByte(0, Bits.unset(bb.getByte(0), 6));
    ShortPacket.parse(bb, dest.getLength());
  }

  private ShortPacket packet() {
    return new ShortPacket(false, of(dest), pn, new Payload(PingFrame.INSTANCE));
  }

  private ByteBuf buffer(ShortPacket packet) {
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);
    return bb;
  }
}
