package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.Configuration;
import com.protocol7.nettyquic.Futures;
import com.protocol7.nettyquic.streams.StreamListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class QuicServer {

  public static Future<QuicServer> bind(
      final Configuration configuration,
      final InetSocketAddress address,
      final StreamListener streamHandler,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {

    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    return Futures.thenSync(
        bindImpl(configuration, address, streamHandler, certificates, privateKey, scheduler),
        g -> new QuicServer(g, scheduler));
  }

  private static Future<EventExecutorGroup> bindImpl(
      final Configuration configuration,
      final InetSocketAddress address,
      final StreamListener streamHandler,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final ScheduledExecutorService scheduler) {
    final NioEventLoopGroup group = new NioEventLoopGroup();

    final Connections connections =
        new Connections(configuration, certificates, privateKey, scheduler);
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
                ChannelPipeline p = ch.pipeline();
                p.addLast(new ServerHandler(router));
              }
            });

    // Bind and startHandshake to accept incoming connections.
    return Futures.thenSync(b.bind(address), aVoid -> group);
  }

  private final EventExecutorGroup group;
  private final ScheduledExecutorService scheduler;

  private QuicServer(final EventExecutorGroup group, final ScheduledExecutorService scheduler) {
    this.group = group;
    this.scheduler = scheduler;
  }

  public Future<?> close() {
    scheduler.shutdown();

    return group.shutdownGracefully();
  }
}
