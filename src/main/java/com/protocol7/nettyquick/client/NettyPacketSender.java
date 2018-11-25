package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.tls.aead.AEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

public class NettyPacketSender implements PacketSender {
  private final Channel channel;

  public NettyPacketSender(Channel channel) {
    this.channel = channel;
  }

  @Override
  public Future<Void> send(Packet packet, InetSocketAddress address, AEAD aead) {
    ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    return channel.writeAndFlush(new DatagramPacket(bb, address)); // TODO fix
  }

  @Override
  public Future<Void> destroy() {
    return channel.close();
  }
}
