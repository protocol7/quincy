package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.Header;
import com.protocol7.nettyquick.protocol.packets.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.MDC;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {


  private Connection connection;

  public void setConnection(final ClientConnection connection) {
    this.connection = connection;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket msg) throws Exception {
    Packet packet = Packet.parse(msg.content(), connectionId -> connection.lastAckedPacketNumber());

    MDC.put("actor", "client");
    MDC.put("packetnumber", packet.getPacketNumber().toString());
    MDC.put("connectionid", packet.getDestinationConnectionId().toString());

    connection.onPacket(packet);
  }
}