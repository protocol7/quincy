package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.parser.PacketParser;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {


  private ClientConnection connection;
  private final PacketParser parser = new PacketParser();

  public void setConnection(final ClientConnection connection) {
    this.connection = connection;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket msg) throws Exception {
    Packet p = parser.parse(msg.content());

    connection.onPacket(p);
  }
}