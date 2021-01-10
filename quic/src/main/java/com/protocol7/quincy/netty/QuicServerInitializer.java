package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.addressvalidation.QuicTokenHandler;
import com.protocol7.quincy.streams.StreamHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import java.security.PrivateKey;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuicServerInitializer extends ChannelInitializer<DatagramChannel> {

  private final Logger log = LoggerFactory.getLogger(QuicServerInitializer.class);

  private final Configuration configuration;
  private final Optional<ChannelHandler> handler;
  private final List<byte[]> certificates;
  private final PrivateKey privateKey;
  private final QuicTokenHandler tokenHandler;
  private final StreamHandler streamHandler;

  public QuicServerInitializer(
      final Configuration configuration,
      final Optional<ChannelHandler> handler,
      final List<byte[]> certificates,
      final PrivateKey privateKey,
      final QuicTokenHandler tokenHandler,
      final StreamHandler streamHandler) {
    this.configuration = configuration;
    this.handler = handler;
    this.certificates = certificates;
    this.privateKey = privateKey;
    this.tokenHandler = tokenHandler;
    this.streamHandler = streamHandler;
  }

  @Override
  protected void initChannel(final DatagramChannel ch) {
    final ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(
        new QuicServerHandler(
            configuration, certificates, privateKey, tokenHandler, streamHandler));

    if (handler.isPresent()) {
      pipeline.addLast(handler.get());
    } else {
      pipeline.addLast(
          new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final ChannelHandlerContext ctx, final Object msg) {}

            @Override
            public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
                throws Exception {
              log.error("Unhandled exception", cause);
            }
          });
    }
  }
}
