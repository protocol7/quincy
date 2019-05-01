/*
 * Copyright 2015 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.protocol7.quincy.http3;

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.UnstableApi;

/** Decorator around another {@link Http2FrameWriter} instance. */
@UnstableApi
public class DecoratingHttp2FrameWriter implements Http2FrameWriter {
  private final Http2FrameWriter delegate;

  public DecoratingHttp2FrameWriter(final Http2FrameWriter delegate) {
    this.delegate = checkNotNull(delegate, "delegate");
  }

  @Override
  public ChannelFuture writeData(
      final ChannelHandlerContext ctx,
      final int streamId,
      final ByteBuf data,
      final int padding,
      final boolean endStream,
      final ChannelPromise promise) {
    return delegate.writeData(ctx, streamId, data, padding, endStream, promise);
  }

  @Override
  public ChannelFuture writeHeaders(
      final ChannelHandlerContext ctx,
      final int streamId,
      final Http2Headers headers,
      final int padding,
      final boolean endStream,
      final ChannelPromise promise) {
    return delegate.writeHeaders(ctx, streamId, headers, padding, endStream, promise);
  }

  @Override
  public ChannelFuture writeHeaders(
      final ChannelHandlerContext ctx,
      final int streamId,
      final Http2Headers headers,
      final int streamDependency,
      final short weight,
      final boolean exclusive,
      final int padding,
      final boolean endStream,
      final ChannelPromise promise) {
    return delegate.writeHeaders(
        ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream, promise);
  }

  @Override
  public ChannelFuture writePriority(
      final ChannelHandlerContext ctx,
      final int streamId,
      final int streamDependency,
      final short weight,
      final boolean exclusive,
      final ChannelPromise promise) {
    return delegate.writePriority(ctx, streamId, streamDependency, weight, exclusive, promise);
  }

  @Override
  public ChannelFuture writeRstStream(
      final ChannelHandlerContext ctx,
      final int streamId,
      final long errorCode,
      final ChannelPromise promise) {
    return delegate.writeRstStream(ctx, streamId, errorCode, promise);
  }

  @Override
  public ChannelFuture writeSettings(
      final ChannelHandlerContext ctx, final Http2Settings settings, final ChannelPromise promise) {
    return delegate.writeSettings(ctx, settings, promise);
  }

  @Override
  public ChannelFuture writeSettingsAck(
      final ChannelHandlerContext ctx, final ChannelPromise promise) {
    return delegate.writeSettingsAck(ctx, promise);
  }

  @Override
  public ChannelFuture writePing(
      final ChannelHandlerContext ctx,
      final boolean ack,
      final long data,
      final ChannelPromise promise) {
    return delegate.writePing(ctx, ack, data, promise);
  }

  @Override
  public ChannelFuture writePushPromise(
      final ChannelHandlerContext ctx,
      final int streamId,
      final int promisedStreamId,
      final Http2Headers headers,
      final int padding,
      final ChannelPromise promise) {
    return delegate.writePushPromise(ctx, streamId, promisedStreamId, headers, padding, promise);
  }

  @Override
  public ChannelFuture writeGoAway(
      final ChannelHandlerContext ctx,
      final int lastStreamId,
      final long errorCode,
      final ByteBuf debugData,
      final ChannelPromise promise) {
    return delegate.writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
  }

  @Override
  public ChannelFuture writeWindowUpdate(
      final ChannelHandlerContext ctx,
      final int streamId,
      final int windowSizeIncrement,
      final ChannelPromise promise) {
    return delegate.writeWindowUpdate(ctx, streamId, windowSizeIncrement, promise);
  }

  @Override
  public ChannelFuture writeFrame(
      final ChannelHandlerContext ctx,
      final byte frameType,
      final int streamId,
      final Http2Flags flags,
      final ByteBuf payload,
      final ChannelPromise promise) {
    return delegate.writeFrame(ctx, frameType, streamId, flags, payload, promise);
  }

  @Override
  public Configuration configuration() {
    return delegate.configuration();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
