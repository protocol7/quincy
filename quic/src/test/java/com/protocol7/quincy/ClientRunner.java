package com.protocol7.quincy;

import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.protocol.Version;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
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
              .withApplicationProtocols("http/0.9")
              .withVersion(Version.DRAFT_29)
              .channelInitializer());

      final Channel channel = b.connect().sync().channel();

      final Connection connection =
          Connection.newBootstrap(channel)
              .withStreamHandler(
                  (stream, data, finished) -> {
                    System.out.println(new String(data));
                  })
              .connect()
              .sync()
              .getNow();

      connection.openStream().write("GET /\r\n".getBytes(), true);

      System.out.println("Connected");

      Thread.sleep(1000);

    } finally {
      workerGroup.shutdownGracefully();
    }
  }
}
