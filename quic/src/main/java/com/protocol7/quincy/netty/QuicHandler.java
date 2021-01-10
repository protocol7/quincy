package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.addressvalidation.QuicTokenHandler;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.Connections;
import com.protocol7.quincy.connection.NettyPacketSender;
import com.protocol7.quincy.connection.PacketRouter;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.StreamHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

public class QuicHandler extends ChannelInboundHandlerAdapter {

  private final Timer timer = new HashedWheelTimer();
  private final PacketRouter router;
  private final Configuration configuration;

  public QuicHandler(
      final Configuration configuration,
      final Optional<List<byte[]>> certificates,
      final Optional<PrivateKey> privateKey,
      final QuicTokenHandler tokenHandler,
      final StreamHandler streamHandler) {
    this.configuration = configuration;
    final Connections connections = new Connections(configuration, timer);
    this.router =
        new PacketRouter(
            configuration.getVersion(),
            connections,
            streamHandler,
            tokenHandler,
            certificates,
            privateKey);
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Timer getTimer() {
    return timer;
  }

  public void putConnection(final ConnectionId dcid, final Connection connection) {
    router.putConnection(dcid, connection);
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof DatagramPacket) {
      final DatagramPacket datagramPacket = (DatagramPacket) msg;
      final ByteBuf bb = datagramPacket.content();
      final List<Packet> packets =
          router.route(
              bb,
              new NettyPacketSender(ctx.channel(), datagramPacket.sender()),
              datagramPacket.sender());

      packets.forEach(ctx::fireChannelRead);

    } else {
      throw new IllegalArgumentException("Expected DatagramPacket packet");
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) {
    timer.stop();

    ctx.fireChannelInactive();
  }
}
