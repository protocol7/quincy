package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.Futures;
import com.protocol7.nettyquic.protocol.Version;
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

public class QuicServer {

  public static Future<QuicServer> bind(
      final Version version,
      final InetSocketAddress address,
      final StreamListener streamHandler,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {
    return Futures.thenSync(
        bindImpl(version, address, streamHandler, certificates, privateKey),
        g -> new QuicServer(g));
  }

  private static Future<EventExecutorGroup> bindImpl(
      final Version version,
      final InetSocketAddress address,
      final StreamListener streamHandler,
      List<byte[]> certificates,
      PrivateKey privateKey) {
    NioEventLoopGroup group = new NioEventLoopGroup();

    Connections connections = new Connections(version, certificates, privateKey);
    PacketRouter router = new PacketRouter(version, connections, streamHandler);

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

  private QuicServer(final EventExecutorGroup group) {
    this.group = group;
  }

  public Future<?> close() {
    return group.shutdownGracefully();
  }
}
