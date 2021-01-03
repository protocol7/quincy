package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.packets.Packet;
import io.netty.util.concurrent.Future;

public interface PacketSender {
  Future<Void> send(Packet packet);

  Future<Void> destroy();
}
