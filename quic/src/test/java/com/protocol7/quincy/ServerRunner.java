package com.protocol7.quincy;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
import com.protocol7.quincy.tls.KeyUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class ServerRunner {

  public static void main(final String[] args) throws InterruptedException {
    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      final Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioDatagramChannel.class);
      b.option(ChannelOption.SO_BROADCAST, true);
      b.handler(
          new QuicBuilder()
              .withCertificates(KeyUtil.getCertsFromCrt("quic/src/test/resources/server.crt"))
              .withPrivateKey(KeyUtil.getPrivateKey("quic/src/test/resources/server.der"))
              .serverChannelInitializer(
                  new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                      System.out.println("############# server got message " + msg);

                      final QuicPacket qp = (QuicPacket) msg;

                      ctx.write(
                          QuicPacket.of(
                              qp.getLocalConnectionId(),
                              qp.getStreamId(),
                              "PONG".getBytes(),
                              qp.sender()));

                      ctx.close();
                      ctx.disconnect();
                    }
                  }));

      b.bind("0.0.0.0", 4444).awaitUninterruptibly();
      System.out.println("Bound");

      Thread.sleep(100000);

    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
