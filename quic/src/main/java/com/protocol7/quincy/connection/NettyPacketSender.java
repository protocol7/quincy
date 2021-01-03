package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.packets.Packet;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

public class NettyPacketSender implements PacketSender {

  private final Channel channel;

  public NettyPacketSender(final Channel channel) {
    this.channel = channel;
  }

  @Override
  public Future<Void> send(final Packet packet) {
    return channel.writeAndFlush(packet);
  }

  @Override
  public Future<Void> destroy() {
    return channel.close();
  }
}
