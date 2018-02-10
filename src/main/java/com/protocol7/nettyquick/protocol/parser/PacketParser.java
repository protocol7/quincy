package com.protocol7.nettyquick.protocol.parser;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.LongPacket;
import com.protocol7.nettyquick.protocol.LongPacketType;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.Version;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang.NotImplementedException;

public class PacketParser {

  public static final int PATCKET_TYPE_MASK = 0b10000000;

  public Packet parse(final ByteBuf bb) {
    byte firstByte = bb.readByte();

    if ((PATCKET_TYPE_MASK & firstByte) == PATCKET_TYPE_MASK) {
      // long packet
      byte ptByte = (byte)((firstByte & (~PATCKET_TYPE_MASK)) & 0xFF);
      LongPacketType packetType = LongPacketType.read(ptByte);
      ConnectionId connId = ConnectionId.read(bb);
      Version version = Version.read(bb);
      PacketNumber packetNumber = PacketNumber.read(bb);

      Payload payload = Payload.parse(bb);

      return new LongPacket(packetType,
                            connId,
                            version,
                            packetNumber,
                            payload);
    } else {
      throw new NotImplementedException();
    }
  }

  public ByteBuf serialize(final Packet p) {
    ByteBuf bb = Unpooled.buffer();

    if (p instanceof LongPacket) {
      LongPacket lp = (LongPacket) p;
      int b = (PATCKET_TYPE_MASK | lp.getPacketType().getType()) & 0xFF;
      bb.writeByte(b);

      lp.getConnectionId().write(bb);

      lp.getVersion().write(bb);

      lp.getPacketNumber().write(bb);

      lp.getPayload().write(bb);
    }

    return bb;
  }

}
