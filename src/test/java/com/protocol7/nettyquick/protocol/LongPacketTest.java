package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.protocol.frames.PingFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

public class LongPacketTest {

  public static final byte[] DATA = "hello".getBytes();

  @Test
  public void roundtrip() {
    PacketType packetType = PacketType.Initial;
    ConnectionId connId = ConnectionId.random();
    PacketNumber pn = PacketNumber.random();
    Version version = Version.DRAFT_09;
    Payload payload = new Payload(new PingFrame(DATA));
    LongPacket lp = new LongPacket(packetType, connId, version, pn, payload);

    ByteBuf bb = Unpooled.buffer();
    lp.write(bb);
    LongPacket parsed = (LongPacket) Packet.parse(bb);

    assertEquals(packetType, parsed.getPacketType());
    assertEquals(connId, parsed.getConnectionId());
    assertEquals(version, parsed.getVersion());
    assertEquals(pn, parsed.getPacketNumber());
    assertEquals(payload, parsed.getPayload());
  }


}