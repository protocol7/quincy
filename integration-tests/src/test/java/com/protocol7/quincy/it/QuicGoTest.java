package com.protocol7.quincy.it;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.tls.extensions.ALPN;
import com.protocol7.testcontainers.quicgo.QuicGoContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.Test;

public class QuicGoTest {

  @Rule public QuicGoContainer quicGo = new QuicGoContainer();

  @Test
  public void get() throws InterruptedException {

    final BlockingQueue<String> responses = new ArrayBlockingQueue<>(10);

    final InetSocketAddress peer = quicGo.getAddress();
    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      final Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioDatagramChannel.class);
      b.remoteAddress(peer);
      b.handler(
          new QuicBuilder()
              .withApplicationProtocols(ALPN.from("hq-29"))
              .withVersion(Version.TLS)
              .withChannelHandler(
                  new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(final ChannelHandlerContext ctx) {
                      ctx.channel().write(QuicPacket.of(null, 0, "GET /\r\n".getBytes(), peer));
                      ctx.fireChannelActive();
                    }
                  })
              .withStreamHandler(
                  (stream, data, finished) -> {
                    responses.add(new String(data, StandardCharsets.US_ASCII));
                  })
              .clientChannelInitializer());

      b.connect();

      // wait for response
      final String response = responses.poll(10000, TimeUnit.MILLISECONDS);

      assertEquals("404 page not found\n", response);

    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
