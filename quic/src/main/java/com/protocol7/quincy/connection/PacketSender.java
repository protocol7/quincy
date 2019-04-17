package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.util.concurrent.Future;

public interface PacketSender {
  Future<Void> send(Packet packet, AEAD aead);

  Future<Void> destroy();
}
