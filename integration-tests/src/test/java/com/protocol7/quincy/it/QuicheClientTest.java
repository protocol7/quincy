package com.protocol7.quincy.it;

import com.protocol7.quincy.netty.QuicBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.output.OutputFrame;

public class QuicheClientTest {

  public static final String ALPN = "http/0.9";

  private EventLoopGroup workerGroup;
  private Bootstrap b;

  @Before
  public void setUp() {
    workerGroup = new NioEventLoopGroup();

    b = new Bootstrap();
    b.group(workerGroup);
    b.channel(NioDatagramChannel.class);
  }

  @Test
  public void get() throws InterruptedException {
    assertGet("ff00001d");
  }

  @Test
  public void verneg() throws InterruptedException {
    assertGet("deadbeef");
  }

  private void assertGet(final String hexVersion) throws InterruptedException {
    b.handler(
        new QuicBuilder()
            .withApplicationProtocols(ALPN)
            .withCertificates(KeyUtil.getCertsFromCrt("src/test/resources/server.crt"))
            .withPrivateKey(KeyUtil.getPrivateKey("src/test/resources/server.der"))
            .withStreamHandler((stream, data, finished) -> stream.write("PONG".getBytes(), true))
            .channelInitializer());

    b.bind("0.0.0.0", 4444).awaitUninterruptibly();

    final BlockingQueue<String> events = new ArrayBlockingQueue<>(10000);

    final QuicheContainer quiche =
        (QuicheContainer)
            new QuicheContainer(false)
                .withEnv("RUST_BACKTRACE", "1")
                .withEnv("RUST_LOG", "trace")
                .withCommand(
                    "/usr/local/bin/quiche-client",
                    "--connect-to=192.168.65.2:4444",
                    "--wire-version=" + hexVersion,
                    "--no-verify",
                    "https://example.org:4444/")
                .withLogConsumer(
                    new Consumer<OutputFrame>() {
                      @Override
                      public void accept(final OutputFrame outputFrame) {
                        events.add(outputFrame.getUtf8String());
                      }
                    });
    quiche.start();

    while (true) {
      final String msg = events.poll(10000, TimeUnit.MILLISECONDS);

      if (msg.contains("1/1 responses received")) {
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
