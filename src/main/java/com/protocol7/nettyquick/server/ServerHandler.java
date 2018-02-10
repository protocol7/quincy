package com.protocol7.nettyquick.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;


public class ServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket packet) throws Exception {
    System.out.println("channelRead0");
    final InetAddress srcAddr = packet.sender().getAddress();
    final ByteBuf buf = packet.content();
    final int rcvPktLength = buf.readableBytes();
    final byte[] rcvPktBuf = new byte[rcvPktLength];
    buf.readBytes(rcvPktBuf);
    System.out.println("Inside incomming packet handler " +  this);

    ByteBuf bb = Unpooled.copiedBuffer("Pong".getBytes());

    InetSocketAddress respAddr = new InetSocketAddress(srcAddr, packet.sender().getPort());

    DatagramPacket resp = new DatagramPacket(bb, respAddr);

    ctx.writeAndFlush(resp);

    System.out.println(respAddr);
  }
}