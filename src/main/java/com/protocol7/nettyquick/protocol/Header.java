package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public interface Header {

  interface LastPacketNumberProvider {
    PacketNumber getLastAcked(ConnectionId connectionId);
  }

  static Header addFrame(Header packet, Frame frame) {
    if (packet instanceof ShortHeader) {
      return ShortHeader.addFrame((ShortHeader) packet, frame);
    } else {
      return LongHeader.addFrame((LongHeader) packet, frame);
    }
  }

  // TODO how does this work when omitting the conn id?
  Optional<ConnectionId> getDestinationConnectionId();

  PacketNumber getPacketNumber();

  PacketType getPacketType();

  void write(ByteBuf bb);
}
