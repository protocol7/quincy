package com.protocol7.quincy;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;

public class ClientRunner {

  public static void main(final String[] args) throws InterruptedException {
    final InetSocketAddress peer = new InetSocketAddress("127.0.0.1", 4433);

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
                      System.out.println("############# sending hello world");

                      ctx.channel().write(QuicPacket.of(null, 0, "PING".getBytes(), peer));

                      ctx.fireChannelActive();
                    }

                    @Override
                    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                      System.out.println("############# got message " + msg);

                      ctx.close();
                      ctx.disconnect();
                    }
                  }));

      final Channel channel = b.connect().syncUninterruptibly().channel();
      System.out.println("Connected");

      Thread.sleep(1000);

    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
