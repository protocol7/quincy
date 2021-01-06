package com.protocol7.quincy;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.extensions.ALPN;
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
              .withApplicationProtocols(ALPN.from("http/0.9"))
              .withVersion(Version.DRAFT_29)
              .withChannelHandler(
                  new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(final ChannelHandlerContext ctx) {
                      System.out.println("sending GET");

                      ctx.channel().write(QuicPacket.of(null, 0, "GET /\r\n".getBytes(), peer));

                      ctx.fireChannelActive();
                    }
                  })
              .withStreamHandler(
                  new StreamHandler() {
                    @Override
                    public void onData(
                        final Stream stream, final byte[] data, final boolean finished) {
                      System.out.println(new String(data));
                    }
                  })
              .clientChannelInitializer());

      final Channel channel = b.connect().syncUninterruptibly().channel();
      System.out.println("Connected");

      Thread.sleep(1000);

    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
