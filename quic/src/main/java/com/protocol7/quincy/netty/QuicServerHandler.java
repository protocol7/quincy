package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.NettyPacketSender;
import com.protocol7.quincy.netty2.api.QuicTokenHandler;
import com.protocol7.quincy.server.Connections;
import com.protocol7.quincy.server.PacketRouter;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;

public class QuicServerHandler extends ChannelDuplexHandler {

  private final Timer timer = new HashedWheelTimer();
  private final StreamListener streamListener =
      new StreamListener() {
        @Override
        public void onData(final Stream stream, final byte[] data, final boolean finished) {
          System.out.println("onData " + new String(data));

          //              ctx.fireChannelRead(
          //                      new QuicPacket(
          //                              stream.getId().getValue(), Unpooled.wrappedBuffer(data),
          // remoteAddress()));
        }
      };

  private final Connections connections;
  private final PacketRouter router;

  public QuicServerHandler(
      final Configuration configuration,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final QuicTokenHandler tokenHandler) {
    this.connections =
        new Connections(configuration, certificates, privateKey, timer, tokenHandler);
    this.router = new PacketRouter(configuration.getVersion(), connections, streamListener);
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof ByteBuf) {
      final ByteBuf bb = (ByteBuf) msg;
      router.route(
          bb,
          new NettyPacketSender(ctx.channel()),
          (InetSocketAddress) ctx.channel().remoteAddress());

    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void write(
      final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
    if (msg instanceof QuicPacket) {
      final QuicPacket qp = (QuicPacket) msg;
      final byte[] data = Bytes.drainToArray(qp.content());

      final Optional<Connection> connection = connections.get(qp.getLocalConnectionId());

      if (connection.isPresent()) {
        connection.get().openStream().write(data, true);
      }
    } else {
      ctx.write(msg, promise);
    }
  }
}
