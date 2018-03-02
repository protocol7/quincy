package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

public interface Packet {

  int PACKET_TYPE_MASK = 0b10000000;

  interface LastPacketNumberProvider {
    PacketNumber getLastAcked(ConnectionId connectionId);
  }

  static Packet parse(ByteBuf bb, LastPacketNumberProvider lastAcked) {
    byte firstByte = bb.getByte(bb.readerIndex());

    if ((PACKET_TYPE_MASK & firstByte) == PACKET_TYPE_MASK) {
      return LongPacket.parse(bb);
    } else {
      // short packet
      return ShortPacket.parse(bb, lastAcked);
    }
  }

  static Packet addFrame(Packet packet, Frame frame) {
    if (packet instanceof ShortPacket) {
      return ShortPacket.addFrame((ShortPacket) packet, frame);
    } else {
      return LongPacket.addFrame((LongPacket) packet, frame);
    }
  }

  // TODO how does this work when omitting the conn id?
  ConnectionId getConnectionId();

  PacketNumber getPacketNumber();

  PacketType getPacketType();

  Payload getPayload();

  void write(ByteBuf bb);
}
