package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Header;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.tls.AEAD;
import com.protocol7.nettyquick.tls.AEADProvider;
import com.protocol7.nettyquick.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.MDC;

import java.util.function.Consumer;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {


  private Connection connection;

  public void setConnection(final ClientConnection connection) {
    this.connection = connection;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket msg) {
    ByteBuf bb = msg.content();
    while (bb.isReadable()) {
      Bytes.debug(bb);
      Packet packet = Packet.parse(msg.content(), connectionId -> connection.lastAckedPacketNumber(),
              (connId, packetType) -> connection.getAEAD(packetType));

      MDC.put("actor", "client");
      if (packet instanceof FullPacket) {
        MDC.put("packetnumber", ((FullPacket) packet).getPacketNumber().toString());
      }
      packet.getDestinationConnectionId().ifPresent(connId -> MDC.put("connectionid", connId.toString()));

      connection.onPacket(packet);
    }
  }
}