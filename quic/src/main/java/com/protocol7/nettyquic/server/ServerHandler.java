package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.connection.NettyPacketSender;
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
    router.route(
        datagram.content(),
        new NettyPacketSender(ctx.channel(), datagram.sender()),
        datagram.sender());
  }
}
