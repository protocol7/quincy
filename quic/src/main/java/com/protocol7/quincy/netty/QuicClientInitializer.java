package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;

public class QuicClientInitializer extends ChannelInitializer<DatagramChannel> {

  private final Configuration configuration;
  private final ChannelHandler handler;

  public QuicClientInitializer(final Configuration configuration, final ChannelHandler handler) {
    this.configuration = configuration;
    this.handler = handler;
  }

  @Override
  protected void initChannel(final DatagramChannel ch) {
    final ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new QuicClientHandler(configuration));
    pipeline.addLast(handler);
  }
}
