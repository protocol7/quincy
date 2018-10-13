package com.protocol7.nettyquick.protocol;

public interface LastPacketNumber {
  PacketNumber getLastAcked(ConnectionId connectionId);
}
