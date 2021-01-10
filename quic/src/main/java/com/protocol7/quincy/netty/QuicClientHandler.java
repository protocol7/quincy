package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HalfParsedPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.StreamHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.slf4j.MDC;

public class QuicClientHandler extends ChannelInboundHandlerAdapter {

  private Connection connection;
  private final Configuration configuration;
  private final Timer timer = new HashedWheelTimer();
  private final StreamHandler streamHandler;

  public QuicClientHandler(final Configuration configuration, final StreamHandler streamHandler) {
    this.configuration = configuration;
    this.streamHandler = streamHandler;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Timer getTimer() {
    return timer;
  }

  public void putConnection(final Connection connection) {
    this.connection = connection;
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof DatagramPacket) {
      if (connection != null) {
        final DatagramPacket datagramPacket = (DatagramPacket) msg;
        final ByteBuf bb = datagramPacket.content();

        final HalfParsedPacket<?> halfParsed = Packet.parse(bb, ConnectionId.LENGTH);

        final Packet packet = halfParsed.complete(connection::getAEAD);

        MDC.put("actor", "client");
        if (packet instanceof FullPacket) {
          MDC.put("packetnumber", Long.toString(((FullPacket) packet).getPacketNumber()));
        }
        MDC.put("connectionid", packet.getDestinationConnectionId().toString());

        connection.onPacket(packet);

        ctx.fireChannelRead(packet);
      } else {
        throw new IllegalStateException("No active connection");
      }
    } else {
      throw new IllegalArgumentException("Expected HalfParsedPacket message");
    }
  }
}
