package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.client.ClientConnection;
import com.protocol7.quincy.connection.NettyPacketSender;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HalfParsedPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.Stream;
import com.protocol7.quincy.streams.StreamListener;
import com.protocol7.quincy.tls.NoopCertificateValidator;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.slf4j.MDC;

public class QuicClientHandler extends ChannelDuplexHandler {

  private ChannelHandlerContext ctx;
  private ClientConnection connection;
  private final Configuration configuration;
  private final Timer timer = new HashedWheelTimer();

  private final StreamListener streamListener =
      new StreamListener() {
        @Override
        public void onData(final Stream stream, final byte[] data, final boolean finished) {
          ctx.fireChannelRead(
              QuicPacket.of(
                  connection.getLocalConnectionId(),
                  stream.getId(),
                  data,
                  connection.getPeerAddress()));
        }
      };

  public QuicClientHandler(final Configuration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) {
    this.ctx = ctx;
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    final ClientConnection connection =
        new ClientConnection(
            configuration,
            ConnectionId.random(),
            streamListener,
            new NettyPacketSender(ctx.channel(), remoteAddress()),
            new DefaultFlowControlHandler(
                configuration.getInitialMaxData(), configuration.getInitialMaxStreamDataUni()),
            (InetSocketAddress) ctx.channel().remoteAddress(),
            new NoopCertificateValidator(), // cert validation disabled
            timer);

    final Promise<Void> handshakePromise = ctx.newPromise();

    connection.handshake(handshakePromise);
    this.connection = connection;

    handshakePromise.addListener(future -> ctx.fireChannelActive());
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) {
    connection.close();

    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof DatagramPacket) {
      final DatagramPacket dg = (DatagramPacket) msg;

      final ByteBuf bb = dg.content();

      while (bb.isReadable()) {
        final HalfParsedPacket<?> halfParsed =
            Packet.parse(bb, connection.getLastDestConnectionIdLength());

        final Packet packet = halfParsed.complete(connection::getAEAD);

        MDC.put("actor", "client");
        if (packet instanceof FullPacket) {
          MDC.put("packetnumber", Long.toString(((FullPacket) packet).getPacketNumber()));
        }
        MDC.put("connectionid", packet.getDestinationConnectionId().toString());

        connection.onPacket(packet);
      }
    } else {
      ctx.fireChannelRead(msg);
    }
  }

  @Override
  public void write(
      final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
    if (msg instanceof QuicPacket) {
      final QuicPacket qp = (QuicPacket) msg;
      final byte[] data = Bytes.drainToArray(qp.content());

      connection.openStream().write(data, true);
    } else {
      ctx.write(msg, promise);
    }
  }

  private InetSocketAddress remoteAddress() {
    return (InetSocketAddress) ctx.channel().remoteAddress();
  }
}
