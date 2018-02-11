package com.protocol7.nettyquick.protocol.parser;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.LongPacket;
import com.protocol7.nettyquick.protocol.LongPacketType;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang.NotImplementedException;

public class PacketParser {

  public static final int PACKET_TYPE_MASK = 0b10000000;

  public Packet parse(final ByteBuf bb) {
    byte firstByte = bb.readByte();

    Bytes.debug("After first byte ", bb);

    if ((PACKET_TYPE_MASK & firstByte) == PACKET_TYPE_MASK) {
      // long packet
      byte ptByte = (byte)((firstByte & (~PACKET_TYPE_MASK)) & 0xFF);
      LongPacketType packetType = LongPacketType.read(ptByte);
      ConnectionId connId = ConnectionId.read(bb);
      Bytes.debug("After connid ", bb);
      Version version = Version.read(bb);
      Bytes.debug("After version ", bb);
      PacketNumber packetNumber = PacketNumber.read(bb);
      Bytes.debug("After packet number ", bb);

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
      int b = (PACKET_TYPE_MASK | lp.getPacketType().getType()) & 0xFF;
      bb.writeByte(b);

      Bytes.debug("Write first byte ", bb);

      lp.getConnectionId().write(bb);
      Bytes.debug("Write connid ", bb);

      lp.getVersion().write(bb);
      Bytes.debug("Write version ", bb);

      lp.getPacketNumber().write(bb);
      Bytes.debug("Write packet number ", bb);

      lp.getPayload().write(bb);
      Bytes.debug("Write payload ", bb);
    }

    return bb;
  }

}
