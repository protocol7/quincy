package com.protocol7.nettyquick.client;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.DatagramPacket;

public class Stream {

  public interface StreamListener {
    void onData(byte[] data);
  }

  private final Channel channel;
  private final InetSocketAddress serverAddress;

  public Stream(final Channel channel, final InetSocketAddress serverAddress) {
    this.channel = channel;
    this.serverAddress = serverAddress;
  }

  public ChannelFuture write(byte[] b) {
    ByteBuf bb = Unpooled.copiedBuffer(b);
    return channel.writeAndFlush(new DatagramPacket(bb, serverAddress));
  }
}
