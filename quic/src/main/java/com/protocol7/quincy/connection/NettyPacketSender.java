package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.aead.AEAD;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;

public class NettyPacketSender implements PacketSender {

  private final Channel channel;

  public NettyPacketSender(final Channel channel) {
    this.channel = channel;
  }

  @Override
  public Future<Void> send(final Packet packet, final AEAD aead) {
    final ByteBuf bb = Unpooled.buffer();
    packet.write(bb, aead);

    return channel.writeAndFlush(bb);
  }

  @Override
  public Future<Void> destroy() {
    return channel.close();
  }
}
