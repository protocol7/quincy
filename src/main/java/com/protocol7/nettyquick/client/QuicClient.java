package com.protocol7.nettyquick.client;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import com.google.common.collect.Maps;
import com.protocol7.nettyquick.protocol.StreamId;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class QuicClient {

  public static QuicClient connect(InetSocketAddress serverAddress) throws InterruptedException, SocketException, UnknownHostException {
    QuicClient client = new QuicClient(serverAddress);

    return client;
  }

  private final InetSocketAddress serverAddress;
  private final Channel channel;
  private final Map<StreamId, Stream.StreamListener> listeners = Maps.newConcurrentMap();
  private final EventLoopGroup group;

  private QuicClient(final InetSocketAddress serverAddress) throws InterruptedException, SocketException, UnknownHostException {
    this.serverAddress = serverAddress;

    this.group = new NioEventLoopGroup();

    Bootstrap b = new Bootstrap();
    b.group(group)
            .channel(NioDatagramChannel.class)
            .handler(new ClientHandler(listeners));

    this.channel = b.bind(0).sync().await().channel();
  }

  public Stream openStream(Stream.StreamListener listener) {
    StreamId streamId = StreamId.create();

    listeners.put(streamId, listener);

    return new Stream(channel, serverAddress);
  }

  public void close() {
    // TODO fix
    channel.close().syncUninterruptibly().awaitUninterruptibly();
    group.shutdownGracefully().syncUninterruptibly().awaitUninterruptibly();
  }
}