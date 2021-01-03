package com.protocol7.quincy.netty;

import com.protocol7.quincy.netty2.api.QuicChannel;
import com.protocol7.quincy.netty2.api.QuicChannelConfig;
import com.protocol7.quincy.netty2.api.QuicConnectionStats;
import com.protocol7.quincy.netty2.api.QuicStreamChannel;
import com.protocol7.quincy.netty2.api.QuicStreamType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.net.SocketAddress;

public class QuicConnectionChannel extends AbstractChannel implements QuicChannel {

  protected QuicConnectionChannel(final Channel parent) {
    super(parent);
  }

  @Override
  public QuicChannelConfig config() {
    return null;
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public ChannelMetadata metadata() {
    return null;
  }

  @Override
  public Future<QuicStreamChannel> createStream(
      final QuicStreamType type,
      final ChannelHandler handler,
      final Promise<QuicStreamChannel> promise) {
    return null;
  }

  @Override
  public byte[] applicationProtocol() {
    return new byte[0];
  }

  @Override
  public ChannelFuture close(
      final boolean applicationClose,
      final int error,
      final ByteBuf reason,
      final ChannelPromise promise) {
    return null;
  }

  @Override
  public Future<QuicConnectionStats> collectStats(final Promise<QuicConnectionStats> promise) {
    return null;
  }

  @Override
  protected AbstractUnsafe newUnsafe() {
    return null;
  }

  @Override
  protected boolean isCompatible(final EventLoop eventLoop) {
    return parent().eventLoop() == eventLoop;
  }

  @Override
  protected SocketAddress localAddress0() {
    return parent().localAddress();
  }

  @Override
  protected SocketAddress remoteAddress0() {
    return parent().remoteAddress();
  }

  @Override
  protected void doBind(final SocketAddress socketAddress) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void doDisconnect() throws Exception {}

  @Override
  protected void doClose() throws Exception {}

  @Override
  protected void doBeginRead() throws Exception {}

  @Override
  protected void doWrite(final ChannelOutboundBuffer channelOutboundBuffer) throws Exception {}
}
