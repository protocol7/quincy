package com.protocol7.nettyquick.protocol.parser;

import static org.junit.Assert.assertEquals;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.LongPacket;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.Version;
import io.netty.buffer.ByteBuf;
import org.junit.Test;

public class PacketParserTest {

  @Test
  public void roundtripLongPacket() {
    PacketType packetType = PacketType.Initial;
    ConnectionId connId = ConnectionId.random();
    PacketNumber pn = PacketNumber.random();
    Version version = Version.DRAFT_09;
    Payload payload = Payload.EMPTY;
    LongPacket lp = new LongPacket(packetType, connId, version, pn, payload);

    PacketParser parser = new PacketParser();

    ByteBuf bb = parser.serialize(lp);

    LongPacket parsed = (LongPacket) parser.parse(bb);

    assertEquals(packetType, parsed.getPacketType());
    assertEquals(connId, parsed.getConnectionId());
    assertEquals(version, parsed.getVersion());
    assertEquals(pn, parsed.getPacketNumber());
    assertEquals(payload, parsed.getPayload());
  }

}