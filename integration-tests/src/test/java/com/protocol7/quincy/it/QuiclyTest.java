package com.protocol7.quincy.it;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.testcontainers.quicly.QuiclyPacket;
import com.protocol7.testcontainers.quicly.QuiclyServerContainer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class QuiclyTest {

  @Rule public QuiclyServerContainer quicly = new QuiclyServerContainer();

  @Test
  public void quiclyServer() throws InterruptedException {

    final BlockingQueue<byte[]> capturedData = new ArrayBlockingQueue<>(10);

    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      final Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioDatagramChannel.class);
      b.remoteAddress(quicly.getAddress());
      b.handler(
          new QuicBuilder()
              .withVersion(Version.DRAFT_29)
              .withChannelHandler(
                  new ChannelDuplexHandler() {
                    @Override
                    public void channelActive(final ChannelHandlerContext ctx) {
                      ctx.channel()
                          .write(
                              QuicPacket.of(
                                  0,
                                  "Hello world".getBytes(),
                                  (InetSocketAddress) ctx.channel().remoteAddress()));

                      ctx.fireChannelActive();
                    }

                    @Override
                    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
                      final QuicPacket packet = (QuicPacket) msg;
                      capturedData.add(Bytes.drainToArray(packet.content()));
                    }
                  })
              .clientChannelInitializer());

      b.connect().awaitUninterruptibly();
      final String actual =
          new String(capturedData.poll(10000, MILLISECONDS), StandardCharsets.US_ASCII);

      // checking that the error message is correct. Shows that we've been able to perform the
      // handshake and opening a stream
      assertTrue(actual.startsWith("HTTP/1.1 500 OK"));

      final List<QuiclyPacket> packets = quicly.getPackets();

      // initial client hello
      assertTrue(packets.get(0).isInbound());

      // initial server hello
      assertFalse(packets.get(1).isInbound());

      // handshake
      assertFalse(packets.get(2).isInbound());

      // handshake ack
      assertTrue(packets.get(3).isInbound());
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
    }
  }
}
