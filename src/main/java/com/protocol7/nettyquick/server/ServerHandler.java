package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.client.NettyPacketSender;
import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.streams.StreamListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import java.util.Optional;
import org.slf4j.MDC;

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
