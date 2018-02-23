package com.protocol7.nettyquick.client;

import java.net.InetSocketAddress;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.utils.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.MDC;

public class QuicClient {

  public static Future<QuicClient> connect(InetSocketAddress serverAddress, StreamListener streamListener) {
    return Futures.thenSync(GlobalEventExecutor.INSTANCE,
                            connectImpl(serverAddress, streamListener),
                            v -> new QuicClient(v));
  }

  private static Future<ClientConnection> connectImpl(final InetSocketAddress serverAddress, StreamListener streamListener) {
    NioEventLoopGroup group = new NioEventLoopGroup();
    ClientHandler handler = new ClientHandler();
    Bootstrap b = new Bootstrap();
    b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(handler);

    Future<Channel> channelFuture = Futures.thenChannel(GlobalEventExecutor.INSTANCE, b.bind(0));

    Future<ClientConnection> conn = Futures.thenSync(GlobalEventExecutor.INSTANCE,
                                                     channelFuture,
                                                     channel -> {
                                                       ClientConnection connection = new ClientConnection(ConnectionId.random(),
                                                                                                          group, channel,
                                                                                                          serverAddress, streamListener);
                                                       handler.setConnection(connection); // TODO fix cyclic creation
                                                       return connection;
                                                     });

    return Futures.thenAsync(GlobalEventExecutor.INSTANCE,
                             conn,
                             clientConnection -> Futures.thenSync(GlobalEventExecutor.INSTANCE, clientConnection.handshake(), aVoid -> clientConnection));
  }

  private final ClientConnection connection;

  private QuicClient(final ClientConnection connection) {
    this.connection = connection;
  }

  public ClientStream openStream() {
    return connection.openStream();
  }

  public Future<?> close() {
    return connection.close();
  }
}