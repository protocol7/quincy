package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.addressvalidation.QuicTokenHandler;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.NettyPacketSender;
import com.protocol7.quincy.server.Connections;
import com.protocol7.quincy.server.PacketRouter;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

public class QuicServerHandler extends ChannelDuplexHandler {

  private final Timer timer = new HashedWheelTimer();

  private final Connections connections;
  private final PacketRouter router;

  public QuicServerHandler(
      final Configuration configuration,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final QuicTokenHandler tokenHandler,
      final StreamHandler streamHandler) {
    this.connections = new Connections(configuration, certificates, privateKey, timer);
    this.router =
        new PacketRouter(configuration.getVersion(), connections, streamHandler, tokenHandler);
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof DatagramPacket) {
      final DatagramPacket datagramPacket = (DatagramPacket) msg;
      final ByteBuf bb = datagramPacket.content();
      router.route(
          bb,
          new NettyPacketSender(ctx.channel(), datagramPacket.sender()),
          datagramPacket.sender());

    } else {
      throw new IllegalArgumentException("Expected DatagramPacket packet");
    }
  }

  @Override
  public void write(
      final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {

    if (msg instanceof QuicPacket) {
      final QuicPacket qp = (QuicPacket) msg;
      final byte[] data = Bytes.drainToArray(qp.content());

      final Optional<Connection> connection = connections.get(qp.getLocalConnectionId());

      connection.ifPresent(value -> value.openStream().write(data, true));

    } else if (msg instanceof DatagramPacket) {
      ctx.write(msg, promise);
    } else {
      throw new IllegalArgumentException("Expected QuicPacket message");
    }
  }
}
