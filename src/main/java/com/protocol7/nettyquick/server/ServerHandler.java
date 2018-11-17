package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.streams.StreamListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.MDC;

import java.util.Optional;


public class ServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final Connections connections = new Connections();
  private final StreamListener streamHandler;

  public ServerHandler(final StreamListener streamHandler) {
    this.streamHandler = streamHandler;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket datagram) throws Exception {
    final ByteBuf bb = datagram.content();

    while (bb.isReadable()) {
      Packet packet = Packet.parse(bb, connectionId -> {
                Optional<Connection> connection = connections.get(connectionId);
                if (connection.isPresent()) {
                  return connection.get().lastAckedPacketNumber();
                } else {
                  throw new IllegalStateException("Connection unknown: " + connectionId);
                }
              },
              (connId, packetType) -> connections.get(connId.get()).get().getAEAD(packetType), -1);

      MDC.put("actor", "server");
      if (packet instanceof FullPacket) {
        MDC.put("packetnumber", ((FullPacket) packet).getPacketNumber().toString());
      }
      if (packet.getDestinationConnectionId().isPresent()) {
        MDC.put("connectionid", packet.getDestinationConnectionId().get().toString());
      }

      ServerConnection conn = connections.get(packet.getDestinationConnectionId(), streamHandler, ctx.channel(), datagram.sender()); // TODO fix for when connId is omitted

      conn.onPacket(packet);
    }
  }
}