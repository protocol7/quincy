package com.protocol7.nettyquick.connection;

import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.tls.AEAD;

public interface Sender {
  void send(Packet packet);
}
