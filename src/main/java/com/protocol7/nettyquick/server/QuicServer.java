package com.protocol7.nettyquick.server;

import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.utils.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

public class QuicServer {

  public static Future<QuicServer> bind(final InetSocketAddress address, StreamListener streamHandler) {
    return Futures.thenSync(bindImpl(address, streamHandler),
                            group1 -> new QuicServer(group1));
  }

  private static Future<EventExecutorGroup> bindImpl(final InetSocketAddress address, final StreamListener streamHandler) {
    NioEventLoopGroup group = new NioEventLoopGroup();

    final Bootstrap b = new Bootstrap();
    b.group(group).channel(NioDatagramChannel.class)
            .option(ChannelOption.SO_BROADCAST, true)
            .handler(new ChannelInitializer<NioDatagramChannel>() {
              @Override
              public void initChannel(final NioDatagramChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new ServerHandler(streamHandler));
              }
            });

    // Bind and start to accept incoming connections.
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