package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.packets.Packet;

public interface Sender {
  void send(Packet packet);
}
