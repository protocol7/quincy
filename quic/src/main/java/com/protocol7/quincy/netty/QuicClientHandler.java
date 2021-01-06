package com.protocol7.quincy.netty;

import com.protocol7.quincy.Configuration;
import com.protocol7.quincy.connection.ClientConnection;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.NettyPacketSender;
import com.protocol7.quincy.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.HalfParsedPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.streams.StreamHandler;
import com.protocol7.quincy.tls.NoopCertificateValidator;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import org.slf4j.MDC;

public class QuicClientHandler extends ChannelDuplexHandler {

  private Connection connection;
  private final Configuration configuration;
  private final Timer timer = new HashedWheelTimer();
  private final StreamHandler streamHandler;

  /*private final StreamHandler streamListener =
  new StreamHandler() {
    @Override
    public void onData(final Stream stream, final byte[] data, final boolean finished) {
      ctx.fireChannelRead(
          QuicPacket.of(
              connection.getSourceConnectionId(),
              stream.getId(),
              data,
              connection.getPeerAddress()));
    }
  };*/

  public QuicClientHandler(final Configuration configuration, final StreamHandler streamHandler) {
    this.configuration = configuration;
    this.streamHandler = streamHandler;
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    final ClientConnection connection =
        new ClientConnection(
            configuration,
            ConnectionId.random(),
            ConnectionId.random(),
            streamHandler,
            new NettyPacketSender(ctx.channel()),
            new DefaultFlowControlHandler(
                configuration.getInitialMaxData(), configuration.getInitialMaxStreamDataUni()),
            (InetSocketAddress) ctx.channel().remoteAddress(),
            new NoopCertificateValidator(), // cert validation disabled
            timer);

    this.connection = connection;

    final Promise<Void> handshakePromise = ctx.newPromise();
    connection.handshake(handshakePromise);
    handshakePromise.addListener(future -> ctx.fireChannelActive());
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) {
    connection.close();

    ctx.fireChannelInactive();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
    if (msg instanceof HalfParsedPacket) {
      final HalfParsedPacket<?> halfParsed = (HalfParsedPacket) msg;

      final Packet packet = halfParsed.complete(connection::getAEAD);

      MDC.put("actor", "client");
      if (packet instanceof FullPacket) {
        MDC.put("packetnumber", Long.toString(((FullPacket) packet).getPacketNumber()));
      }
      MDC.put("connectionid", packet.getDestinationConnectionId().toString());

      connection.onPacket(packet);

      ctx.fireChannelRead(packet);

    } else {
      throw new IllegalArgumentException("Expected HalfParsedPacket message");
    }
  }

  @Override
  public void write(
      final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) {
    if (msg instanceof Packet) {
      final Packet packet = (Packet) msg;

      final ByteBuf bb = Unpooled.buffer();
      packet.write(bb, connection.getAEAD(Packet.getEncryptionLevel(packet)));

      ctx.write(bb, promise);
    } else if (msg instanceof QuicPacket) {
      final QuicPacket qp = (QuicPacket) msg;
      final byte[] data = Bytes.drainToArray(qp.content());

      connection.openStream().write(data, true);
    } else {
      throw new IllegalArgumentException("Expected Packet message");
    }
  }
}
