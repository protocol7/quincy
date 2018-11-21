package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.tls.aead.AEAD;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

public interface PacketSender {
  Future<Void> send(Packet packet, InetSocketAddress address, AEAD aead);

  Future<Void> destroy();
}
