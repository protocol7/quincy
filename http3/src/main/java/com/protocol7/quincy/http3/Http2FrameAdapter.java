/*
 * Copyright 2014 The Netty Project
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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.UnstableApi;

/**
 * Convenience class that provides no-op implementations for all methods of {@link
 * Http2FrameListener}.
 */
@UnstableApi
public class Http2FrameAdapter implements Http2FrameListener {

  @Override
  public int onDataRead(
      final ChannelHandlerContext ctx,
      final int streamId,
      final ByteBuf data,
      final int padding,
      final boolean endOfStream)
      throws Http2Exception {
    return data.readableBytes() + padding;
  }

  @Override
  public void onHeadersRead(
      final ChannelHandlerContext ctx,
      final int streamId,
      final Http2Headers headers,
      final int padding,
      final boolean endStream)
      throws Http2Exception {}

  @Override
  public void onHeadersRead(
      final ChannelHandlerContext ctx,
      final int streamId,
      final Http2Headers headers,
      final int streamDependency,
      final short weight,
      final boolean exclusive,
      final int padding,
      final boolean endStream)
      throws Http2Exception {}

  @Override
  public void onPriorityRead(
      final ChannelHandlerContext ctx,
      final int streamId,
      final int streamDependency,
      final short weight,
      final boolean exclusive)
      throws Http2Exception {}

  @Override
  public void onRstStreamRead(
      final ChannelHandlerContext ctx, final int streamId, final long errorCode)
      throws Http2Exception {}

  @Override
  public void onSettingsAckRead(final ChannelHandlerContext ctx) throws Http2Exception {}

  @Override
  public void onSettingsRead(final ChannelHandlerContext ctx, final Http2Settings settings)
      throws Http2Exception {}

  @Override
  public void onPingRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {}

  @Override
  public void onPingAckRead(final ChannelHandlerContext ctx, final long data)
      throws Http2Exception {}

  @Override
  public void onPushPromiseRead(
      final ChannelHandlerContext ctx,
      final int streamId,
      final int promisedStreamId,
      final Http2Headers headers,
      final int padding)
      throws Http2Exception {}

  @Override
  public void onGoAwayRead(
      final ChannelHandlerContext ctx,
      final int lastStreamId,
      final long errorCode,
      final ByteBuf debugData)
      throws Http2Exception {}

  @Override
  public void onWindowUpdateRead(
      final ChannelHandlerContext ctx, final int streamId, final int windowSizeIncrement)
      throws Http2Exception {}

  @Override
  public void onUnknownFrame(
      final ChannelHandlerContext ctx,
      final byte frameType,
      final int streamId,
      final Http2Flags flags,
      final ByteBuf payload) {}
}
