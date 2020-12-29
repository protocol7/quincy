package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.logging.LoggingHandler;
import java.security.PrivateKey;
import java.util.List;

public class QuicServerInitializer extends ChannelInitializer<DatagramChannel> {

  private final Configuration configuration;
  private final ChannelHandler handler;
  private final List<byte[]> certificates;
  private final PrivateKey privateKey;

  public QuicServerInitializer(
      final Configuration configuration,
      final ChannelHandler handler,
      final List<byte[]> certificates,
      final PrivateKey privateKey) {
    this.configuration = configuration;
    this.handler = handler;
    this.certificates = certificates;
    this.privateKey = privateKey;
  }

  @Override
  protected void initChannel(final DatagramChannel ch) {
    final ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new LoggingHandler());
    pipeline.addLast(new QuicServerHandler(configuration, certificates, privateKey));
    pipeline.addLast(handler);
  }
}
