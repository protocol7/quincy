package com.protocol7.nettyquick.connection;

import com.protocol7.nettyquick.protocol.packets.Packet;

public interface Sender {
  void send(Packet packet);
}
