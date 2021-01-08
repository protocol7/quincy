package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.streams.StreamHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import java.util.Optional;

public class QuicClientInitializer extends ChannelInitializer<DatagramChannel> {

  private final Configuration configuration;
  private final Optional<ChannelHandler> handler;
  private final StreamHandler streamHandler;

  public QuicClientInitializer(
      final Configuration configuration,
      final Optional<ChannelHandler> handler,
      final StreamHandler streamHandler) {
    this.configuration = configuration;
    this.handler = handler;
    this.streamHandler = streamHandler;
  }

  @Override
  protected void initChannel(final DatagramChannel ch) {
    final ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(new DatagramPacketHandler());
    pipeline.addLast(new QuicConnectionHandler());
    pipeline.addLast(new QuicClientHandler(configuration, streamHandler));

    if (handler.isPresent()) {
      pipeline.addLast(handler.get());
    } else {
      pipeline.addLast(
          new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg)
                throws Exception {}
          });
    }
  }
}
