package com.protocol7.nettyquick.protocol;

public interface Packet {

  // TODO how does this work when omitting the conn id?
  ConnectionId getConnectionId();

  PacketType getPacketType();

  Payload getPayload();
}
