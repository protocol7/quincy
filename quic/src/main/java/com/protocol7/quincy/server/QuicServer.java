package com.protocol7.quincy.server;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.Futures;
import com.protocol7.quincy.streams.StreamListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;

public class QuicServer {

  public static Future<QuicServer> bind(
      final Configuration configuration,
      final InetSocketAddress address,
      final StreamListener streamHandler,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {

    return Futures.thenSync(
        bindImpl(configuration, address, streamHandler, certificates, privateKey),
        g -> new QuicServer(g));
  }

  private static Future<EventExecutorGroup> bindImpl(
      final Configuration configuration,
      final InetSocketAddress address,
      final StreamListener streamHandler,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {
    final NioEventLoopGroup group = new NioEventLoopGroup();

    final Timer timer = new HashedWheelTimer();

    final Connections connections = new Connections(configuration, certificates, privateKey, timer);
    final PacketRouter router =
        new PacketRouter(configuration.getVersion(), connections, streamHandler);

    final Bootstrap b = new Bootstrap();
    b.group(group)
        .channel(NioDatagramChannel.class)
        .option(ChannelOption.SO_BROADCAST, true)
        .handler(
            new ChannelInitializer<NioDatagramChannel>() {
              @Override
              public void initChannel(final NioDatagramChannel ch) {
                final ChannelPipeline p = ch.pipeline();
                p.addLast(new ServerHandler(router));
              }
            });

    // Bind and startHandshake to accept incoming connections.
    return Futures.thenSync(b.bind(address), aVoid -> group);
  }

  private final EventExecutorGroup group;

  private QuicServer(final EventExecutorGroup group) {
    this.group = group;
  }

  public Future<?> close() {
    return group.shutdownGracefully();
  }
}
