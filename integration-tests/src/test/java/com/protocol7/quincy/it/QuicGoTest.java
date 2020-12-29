package com.protocol7.quincy.it;

import static org.junit.Assert.assertEquals;

import com.protocol7.quincy.netty.QuicBuilder;
import com.protocol7.quincy.netty.QuicPacket;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.testcontainers.quicgo.QuicGoContainer;
import com.protocol7.testcontainers.quicgo.QuicGoPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class QuicGoTest {

  @Rule public QuicGoContainer quicGo = new QuicGoContainer();

  @Test
  public void test() throws InterruptedException {
    final EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      final Bootstrap b = new Bootstrap();
      b.group(workerGroup);
      b.channel(NioDatagramChannel.class);
      b.remoteAddress(quicGo.getAddress());
      b.handler(
          new QuicBuilder()
              .withVersion(Version.DRAFT_29)
              .clientChannelInitializer(
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
                  }));

      b.connect().awaitUninterruptibly();

      Thread.sleep(2000);
    } finally {
      if (workerGroup != null) {
        workerGroup.shutdownGracefully();
      }
    }

    final List<QuicGoPacket> packets = quicGo.getPackets();

    // client hello without token
    assertPacket(packets.get(0), true, true, "Initial", 0);

    // token needed, retry
    assertPacket(packets.get(1), false, true, "Retry", -1);

    // client hello with token
    assertPacket(packets.get(2), true, true, "Initial", 1);

    // server hello
    assertPacket(packets.get(3), false, true, "Initial", 0);

    // handshake
    assertPacket(packets.get(4), false, true, "Handshake", 0);

    assertPacket(packets.get(5), false, false, "1-RTT", 0);

    // ack handshake
    assertPacket(packets.get(6), true, true, "Handshake", 2);

    // ack handshake
    assertPacket(packets.get(7), false, true, "Handshake", 1);
  }

  private void assertPacket(
      final QuicGoPacket actual,
      final boolean inbount,
      final boolean longHeader,
      final String type,
      final long pn) {
    assertEquals(inbount, actual.inbound);
    assertEquals(longHeader, actual.longHeader);
    assertEquals(type, actual.type);
    assertEquals(pn, actual.packetNumber);
  }
}
