package com.protocol7.quincy.it;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
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

public class QuicheTest {

  @Rule public QuicheContainer quiche = new QuicheContainer();

  @Test
  public void get() throws InterruptedException {

    final BlockingQueue<String> responses = new ArrayBlockingQueue<>(10);

    final InetSocketAddress peer = quiche.getAddress();
    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      final Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioDatagramChannel.class);
      b.remoteAddress(peer);
      b.handler(
          new QuicBuilder()
              .clientChannelInitializer(
                  new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(final ChannelHandlerContext ctx) {
                      ctx.channel().write(QuicPacket.of(null, 0, "GET /\r\n".getBytes(), peer));
                      ctx.fireChannelActive();
                    }

                    @Override
                    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                      final QuicPacket qp = (QuicPacket) msg;

                      responses.add(qp.content().toString(StandardCharsets.US_ASCII));

                      ctx.close();
                      ctx.disconnect();
                    }
                  }));

      b.connect();

      // wait for response
      final String response = responses.poll(10000, TimeUnit.MILLISECONDS);

      assertEquals("Not Found!\r\n", response);

    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
