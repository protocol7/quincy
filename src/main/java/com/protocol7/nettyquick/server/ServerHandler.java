package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.parser.PacketParser;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;


public class ServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final PacketParser packetParser = new PacketParser();
  private final Connections connections = new Connections();
  private final StreamHandler streamHandler;

  public ServerHandler(final StreamHandler streamHandler) {
    this.streamHandler = streamHandler;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket datagram) throws Exception {
    System.out.println("s got packet");
    final ByteBuf bb = datagram.content();

    Bytes.debug("Server got ", bb);

    Packet packet = packetParser.parse(bb);

    ServerConnection conn = connections.getOrCreate(packet.getConnectionId(), streamHandler, ctx.channel(), datagram.sender()); // TODO fix for when connId is omitted

    conn.onPacket(packet);
  }
}