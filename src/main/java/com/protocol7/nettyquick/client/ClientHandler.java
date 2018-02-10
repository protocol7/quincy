package com.protocol7.nettyquick.client;

import java.util.Map;

import com.protocol7.nettyquick.protocol.StreamId;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

public class ClientHandler extends SimpleChannelInboundHandler<DatagramPacket> {


  private final Map<StreamId, Stream.StreamListener> listeners;

  public ClientHandler(final Map<StreamId, Stream.StreamListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final DatagramPacket msg) throws Exception {
    ByteBuf content = msg.content();
    byte[] b = new byte[content.readableBytes()];
    content.readBytes(b);
    for (Stream.StreamListener listener : listeners.values()) {
      listener.onData(b);
    }
  }
}