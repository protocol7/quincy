package com.protocol7.nettyquick.server;

import java.util.Optional;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.streams.StreamListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.MDC;


public class ServerHandler extends SimpleChannelInboundHandler<DatagramPacket> {

  private final Connections connections = new Connections();
  private final StreamListener streamHandler;

  public ServerHandler(final StreamListener streamHandler) {
    this.streamHandler = streamHandler;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket datagram) throws Exception {
    System.out.println("Server first");

    final ByteBuf bb = datagram.content();

    System.out.println("Server got " + bb);

    Packet packet = Packet.parse(bb, connectionId -> {
      Optional<Connection> connection = connections.get(connectionId);
      if (connection.isPresent()) {
        return connection.get().lastAckedPacketNumber(); // TODO is getting the next the right thing to do?
      } else {
        throw new IllegalStateException("Connection unknown: " + connectionId);
      }
    });

    MDC.put("actor", "server");
    MDC.put("packetnumber", packet.getPacketNumber().toString());
    MDC.put("connectionid", packet.getConnectionId().toString());

    ServerConnection conn = connections.get(packet.getConnectionId(), streamHandler, ctx.channel(), datagram.sender()); // TODO fix for when connId is omitted

    conn.onPacket(packet);
  }
}