package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.packets.Packet;

public interface Sender {
  void send(Packet packet);
}
