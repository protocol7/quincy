package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.Frame;

public interface Packet {

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
}
