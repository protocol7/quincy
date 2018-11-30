package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.client.NettyPacketSender;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class ServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final PacketRouter router;

  public ServerHandler(PacketRouter router) {
    this.router = router;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket datagram) {
    router.route(datagram.content(), datagram.sender(), new NettyPacketSender(ctx.channel()));
  }
}
