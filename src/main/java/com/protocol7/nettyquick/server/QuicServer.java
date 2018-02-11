package com.protocol7.nettyquick.server;

import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class QuicServer {

  public static QuicServer bind(final InetSocketAddress address, StreamHandler streamHandler) {
    return new QuicServer(address, streamHandler);
  }

  private final InetSocketAddress address;
  private final Channel channel;
  private final NioEventLoopGroup group;

  private QuicServer(final InetSocketAddress address, final StreamHandler streamHandler) {
    this.address = address;

    this.group = new NioEventLoopGroup();

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
    System.out.println("Binding to " + address);
    //b.bind(address).sync().channel().closeFuture().await();
    this.channel = b.bind(address).syncUninterruptibly().awaitUninterruptibly().channel();
  }

  public void close() {
    // TODO fix
    channel.close().syncUninterruptibly().awaitUninterruptibly();
    group.shutdownGracefully().syncUninterruptibly().awaitUninterruptibly();
  }
}