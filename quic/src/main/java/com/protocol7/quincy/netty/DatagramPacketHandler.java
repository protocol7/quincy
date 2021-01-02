package com.protocol7.quincy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import java.net.InetSocketAddress;

public class DatagramPacketHandler extends ChannelDuplexHandler {

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof DatagramPacket) {
      final DatagramPacket dg = (DatagramPacket) msg;

      final ByteBuf bb = dg.content();

      ctx.fireChannelRead(bb);
    } else {
      throw new IllegalArgumentException("Expected DatagramPacket message");
    }
  }

  @Override
  public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
    if (msg instanceof ByteBuf) {

      final ByteBuf bb = (ByteBuf) msg;

      ctx.write(new DatagramPacket(bb, (InetSocketAddress) ctx.channel().remoteAddress()), promise);
    } else {
      throw new IllegalArgumentException("Expected ByteBuf message");
    }
  }
}
