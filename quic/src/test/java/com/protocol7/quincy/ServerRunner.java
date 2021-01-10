package com.protocol7.quincy;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.KeyUtil;
import io.netty.bootstrap.Bootstrap;
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
              .withStreamHandler(
                  new StreamHandler() {
                    @Override
                    public void onData(
                        final Stream stream, final byte[] data, final boolean finished) {
                      System.out.println("server got message " + new String(data));

                      stream.write("PONG".getBytes(), true);
                    }
                  })
              .channelInitializer());

      b.bind("0.0.0.0", 4444).awaitUninterruptibly();
      System.out.println("Bound");

      Thread.sleep(100000);

    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
