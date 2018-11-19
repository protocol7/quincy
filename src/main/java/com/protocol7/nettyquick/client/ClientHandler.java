package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.MDC;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {


  private ClientConnection connection;

  public void setConnection(final ClientConnection connection) {
    this.connection = connection;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket msg) {
    ByteBuf bb = msg.content();
    while (bb.isReadable()) {
      Packet packet = Packet.parse(msg.content(), connectionId -> connection.lastAckedPacketNumber(),
              (connId, packetType) -> connection.getAEAD(packetType), connection.getLastDestConnectionIdLength());

      MDC.put("actor", "client");
      if (packet instanceof FullPacket) {
        MDC.put("packetnumber", ((FullPacket) packet).getPacketNumber().toString());
      }
      packet.getDestinationConnectionId().ifPresent(connId -> MDC.put("connectionid", connId.toString()));

      connection.onPacket(packet);
    }
  }
}