package com.protocol7.nettyquick.client;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.streams.StreamListener;
import com.protocol7.nettyquick.utils.Futures;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;

import java.net.InetSocketAddress;

public class QuicClient {

  public static Future<QuicClient> connect(InetSocketAddress serverAddress, StreamListener streamListener) {
    NioEventLoopGroup group = new NioEventLoopGroup();
    ClientHandler handler = new ClientHandler();
    Bootstrap b = new Bootstrap();
    b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(handler);

    Future<Channel> channelFuture = Futures.thenChannel(b.bind(0));

    Future<ClientConnection> conn = Futures.thenSync(channelFuture,
                                                     channel -> {
                                                       ClientConnection connection = new ClientConnection(ConnectionId.random(),
                                                                                                          new NettyPacketSender(channel),
                                                                                                          serverAddress, streamListener);
                                                       handler.setConnection(connection); // TODO fix cyclic creation
                                                       return connection;
                                                     });

    Future<ClientConnection> f = Futures.thenAsync(conn,
                             clientConnection -> Futures.thenSync(clientConnection.handshake(), aVoid -> clientConnection));
    return Futures.thenSync(f, v -> new QuicClient(group, v));
  }

  private final NioEventLoopGroup group;
  private final ClientConnection connection;

  private QuicClient(NioEventLoopGroup group, final ClientConnection connection) {
    this.group = group;
    this.connection = connection;
  }

  public Stream openStream() {
    return connection.openStream();
  }

  public Future<?> close() {
    return Futures.thenAsync(
            connection.close(),
            aVoid -> (Future<Void>) group.shutdownGracefully());
  }
}