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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

    final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
                      new DefaultFlowControlHandler(
                          configuration.getInitialMaxData(),
                          configuration.getInitialMaxStreamDataUni()),
                      serverAddress,
                      scheduler);
              handler.setConnection(connection); // TODO fix cyclic creation
              return connection;
            });

    final Future<ClientConnection> f =
        Futures.thenAsync(
            conn,
            clientConnection ->
                Futures.thenSync(clientConnection.handshake(), aVoid -> clientConnection));

    return Futures.thenSync(f, v -> new QuicClient(group, v, scheduler));
  }

  private final NioEventLoopGroup group;
  private final ClientConnection connection;
  private final ScheduledExecutorService scheduler;

  private QuicClient(
      final NioEventLoopGroup group,
      final ClientConnection connection,
      final ScheduledExecutorService scheduler) {
    this.group = group;
    this.connection = connection;
    this.scheduler = scheduler;
  }

  public Stream openStream() {
    return connection.openStream();
  }

  public Future<?> close() {
    scheduler.shutdown();

    return Futures.thenAsync(
        connection.close(), aVoid -> (Future<Void>) group.shutdownGracefully());
  }
}
