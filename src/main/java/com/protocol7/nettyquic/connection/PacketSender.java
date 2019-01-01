package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.tls.aead.AEAD;
import io.netty.util.concurrent.Future;

public interface PacketSender {
  Future<Void> send(Packet packet, AEAD aead);

  Future<Void> destroy();
}
