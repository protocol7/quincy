package com.protocol7.quincy.it;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.testcontainers.quicly.QuiclyClientContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.Test;

public class QuiclyClientTest {

  @Test
  public void quiclyClient() throws InterruptedException {

    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      final Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioDatagramChannel.class);
      b.option(ChannelOption.SO_BROADCAST, true);
      b.handler(
          new QuicBuilder()
              .withVersion(Version.DRAFT_20)
              .withCertificates(KeyUtil.getCertsFromCrt("src/test/resources/server.crt"))
              .withPrivateKey(KeyUtil.getPrivateKey("src/test/resources/server.der"))
              .serverChannelInitializer(
                  new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                      System.out.println("server got message " + msg);
                    }
                  }));

      b.bind("0.0.0.0", 4444).awaitUninterruptibly();

      Thread.sleep(1000);

      QuiclyClientContainer quicly = null;
      try {
        quicly = new QuiclyClientContainer("host.docker.internal", 4444);
        quicly.withCommand(
            "./cli", "-vv", "-x", "X25519", "host.docker.internal", Integer.toString(4444));
        quicly.start();

        // TODO replace with verification
        Thread.sleep(3000);
      } finally {
        if (quicly != null) {
          quicly.stop();
          quicly.close();
        }
      }
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
    }
  }
}
