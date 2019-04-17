package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;

public class NettyPacketSender implements PacketSender {

  private final Channel channel;
  private final InetSocketAddress peerAddress;

  public NettyPacketSender(final Channel channel, final InetSocketAddress peerAddress) {
    this.channel = channel;
    this.peerAddress = peerAddress;
  }

  @Override
  public Future<Void> send(final Packet packet, final AEAD aead) {
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    return channel.writeAndFlush(new DatagramPacket(bb, peerAddress));
  }

  @Override
  public Future<Void> destroy() {
    return channel.close();
  }
}
