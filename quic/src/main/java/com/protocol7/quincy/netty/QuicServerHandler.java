package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.NettyPacketSender;
import com.protocol7.quincy.server.Connections;
import com.protocol7.quincy.server.PacketRouter;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.utils.Bytes;
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
  private final StreamListener streamListener =
      new StreamListener() {
        @Override
        public void onData(final Stream stream, final byte[] data) {
          System.out.println("onData " + new String(data));

          //              ctx.fireChannelRead(
          //                      new QuicPacket(
          //                              stream.getId().getValue(), Unpooled.wrappedBuffer(data),
          // remoteAddress()));
        }

        @Override
        public void onFinished() {
          System.out.println("onFinished ");
        }

        @Override
        public void onReset(
            final Stream stream, final int applicationErrorCode, final long offset) {
          System.out.println("onReset ");
        }
      };

  private final Connections connections;
  private final PacketRouter router;

  public QuicServerHandler(
      final Configuration configuration,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {
    this.connections = new Connections(configuration, certificates, privateKey, timer);
    this.router = new PacketRouter(configuration.getVersion(), connections, streamListener);
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof DatagramPacket) {
      final DatagramPacket datagram = (DatagramPacket) msg;
      router.route(
          datagram.content(),
          new NettyPacketSender(ctx.channel(), datagram.sender()),
          datagram.sender());

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
