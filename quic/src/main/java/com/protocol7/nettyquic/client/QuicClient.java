package com.protocol7.nettyquic.client;

import com.protocol7.nettyquic.Configuration;
import com.protocol7.nettyquic.Futures;
import com.protocol7.nettyquic.connection.NettyPacketSender;
import com.protocol7.nettyquic.flowcontrol.DefaultFlowControlHandler;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.streams.Stream;
import com.protocol7.nettyquic.streams.StreamListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;

public class QuicClient {

  public static Future<QuicClient> connect(
      final Configuration configuration,
      final InetSocketAddress serverAddress,
      final StreamListener streamListener) {
    final NioEventLoopGroup group = new NioEventLoopGroup();
    final ClientHandler handler = new ClientHandler();
    final Bootstrap b = new Bootstrap();
    b.group(group).channel(NioDatagramChannel.class).handler(handler);

    final Future<Channel> channelFuture = Futures.thenChannel(b.bind(0));

    final Future<ClientConnection> conn =
        Futures.thenSync(
            channelFuture,
            channel -> {
              ClientConnection connection =
                  new ClientConnection(
                      configuration,
                      ConnectionId.random(),
                      streamListener,
                      new NettyPacketSender(channel, serverAddress),
                      new DefaultFlowControlHandler(49152, 32768),
                      serverAddress);
              handler.setConnection(connection); // TODO fix cyclic creation
              return connection;
            });

    final Future<ClientConnection> f =
        Futures.thenAsync(
            conn,
            clientConnection ->
                Futures.thenSync(clientConnection.handshake(), aVoid -> clientConnection));
    return Futures.thenSync(f, v -> new QuicClient(group, v));
  }

  private final NioEventLoopGroup group;
  private final ClientConnection connection;

  private QuicClient(final NioEventLoopGroup group, final ClientConnection connection) {
    this.group = group;
    this.connection = connection;
  }

  public Stream openStream() {
    return connection.openStream();
  }

  public Future<?> close() {
    return Futures.thenAsync(
        connection.close(), aVoid -> (Future<Void>) group.shutdownGracefully());
  }
}
