package com.protocol7.nettyquick.protocol;

public interface Packet {

  // TODO how does this work when omitting the conn id?
  ConnectionId getConnectionId();

  PacketNumber getPacketNumber();

  PacketType getPacketType();

  Payload getPayload();
}
