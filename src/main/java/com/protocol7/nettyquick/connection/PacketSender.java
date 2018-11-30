package com.protocol7.nettyquick.connection;

import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.tls.aead.AEAD;
import io.netty.util.concurrent.Future;

public interface PacketSender {
  Future<Void> send(Packet packet, AEAD aead);

  Future<Void> destroy();
}
