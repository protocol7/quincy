package com.protocol7.quincy.it;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.output.OutputFrame;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.protocol7.quincy.utils.Hex.dehex;
import static org.junit.Assert.assertEquals;

public class QuicheClientTest {

  public static final String ALPN = "http/0.9";

  private final EventLoopGroup workerGroup = new NioEventLoopGroup();
  private final Bootstrap b = new Bootstrap();

  @Before
  public void setUp() {
    b.group(workerGroup);
    b.channel(NioDatagramChannel.class);
  }

  @Test
  public void get() throws InterruptedException {
      final Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioDatagramChannel.class);
      b.handler(
              new QuicBuilder()
                      .withApplicationProtocols(ALPN.getBytes())
                      .withCertificates(KeyUtil.getCertsFromCrt("src/test/resources/server.crt"))
                      .withPrivateKey(KeyUtil.getPrivateKey("src/test/resources/server.der"))
                      .withStreamHandler(
                              new StreamHandler() {
                                @Override
                                public void onData(
                                        final Stream stream, final byte[] data, final boolean finished) {
                                  System.out.println("server got message " + new String(data));

                                  stream.write("PONG".getBytes(), true);
                                }
                              })
                      .serverChannelInitializer());

      b.bind("0.0.0.0", 4444).awaitUninterruptibly();

      final BlockingQueue<String> events = new ArrayBlockingQueue<>(1000);

    final QuicheContainer quiche =
        (QuicheContainer)
            new QuicheContainer(false)
                .withCommand(
                    "/usr/local/bin/quiche-client", "--connect-to=192.168.65.2:4444", "--wire-version=ff00001d", "--no-verify", "https://example.org:4444/")
                .withLogConsumer(
                    new Consumer<OutputFrame>() {
                        @Override
                      public void accept(final OutputFrame outputFrame) {
                        System.out.println(outputFrame.getUtf8String());
                        events.add(outputFrame.getUtf8String());
                      }
                    });
      quiche.start();

      while (true) {
          final String msg = events.poll(10000, TimeUnit.MILLISECONDS);

          if (msg.contains("1/1 response(s) received in")) {
              break;
          }
      }

      quiche.stop();
  }

  @After
  public void teardown() {
    workerGroup.shutdownGracefully();
  }
}
