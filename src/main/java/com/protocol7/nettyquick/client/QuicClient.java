package com.protocol7.nettyquick.client;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.StreamId;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class QuicClient {

  public static QuicClient connect(InetSocketAddress serverAddress, StreamListener streamListener) throws InterruptedException, SocketException, UnknownHostException {
    QuicClient client = new QuicClient(serverAddress, streamListener);

    return client;
  }

  private final ClientConnection connection;
  private final EventLoopGroup group;

  private QuicClient(final InetSocketAddress serverAddress, StreamListener streamListener) throws InterruptedException, SocketException, UnknownHostException {
    this.group = new NioEventLoopGroup();

    ClientHandler handler = new ClientHandler();
    Bootstrap b = new Bootstrap();
    b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(handler);

    this.connection = new ClientConnection(ConnectionId.create(),
                                           b.bind(0).sync().await().channel(),
                                           serverAddress, streamListener);
    handler.setConnection(connection); // TODO fix cyclic creation
  }

  public ClientStream openStream() {
    return connection.openStream();
  }

  public void close() {
    // TODO fix
    connection.close();

    group.shutdownGracefully().syncUninterruptibly().awaitUninterruptibly();
  }
}