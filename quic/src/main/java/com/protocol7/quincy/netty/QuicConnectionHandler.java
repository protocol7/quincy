package com.protocol7.quincy.netty;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.HalfParsedPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

public class QuicConnectionHandler extends ChannelInboundHandlerAdapter {

  private final ConcurrentMap<ConnectionId, QuicConnectionChannel> connections =
      new ConcurrentHashMap<>();

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof ByteBuf && ((ByteBuf) msg).isReadable()) {

      final ByteBuf bb = (ByteBuf) msg;

      final HalfParsedPacket<?> halfParsed = Packet.parse(bb, ConnectionId.LENGTH);

      // final QuicConnectionChannel connection =
      // getOrCreateConnection(halfParsed.getDestinationConnectionId(), ctx.channel());
      // connection.pipeline().fireChannelRead(halfParsed);

      ctx.fireChannelRead(halfParsed);

    } else {
      throw new IllegalArgumentException("Expected readable ByteBuf message");
    }
  }

  private QuicConnectionChannel getOrCreateConnection(
      final ConnectionId connectionId, final Channel parent) {
    return connections.computeIfAbsent(
        connectionId,
        new Function<ConnectionId, QuicConnectionChannel>() {
          @Override
          public QuicConnectionChannel apply(final ConnectionId connectionId) {
            final QuicConnectionChannel channel = new QuicConnectionChannel(parent);
            parent.eventLoop().register(channel);

            return channel;
          }
        });
  }
}
