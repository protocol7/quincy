/*
 * Copyright 2014 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.protocol7.quincy.http3;

import static com.protocol7.quincy.http3.Http2FrameLogger.Direction.INBOUND;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.UnstableApi;

/**
 * Decorator around a {@link Http2FrameReader} that logs all inbound frames before calling back the
 * listener.
 */
@UnstableApi
public class Http2InboundFrameLogger implements Http2FrameReader {
  private final Http2FrameReader reader;
  private final Http2FrameLogger logger;

  public Http2InboundFrameLogger(final Http2FrameReader reader, final Http2FrameLogger logger) {
    this.reader = checkNotNull(reader, "reader");
    this.logger = checkNotNull(logger, "logger");
  }

  @Override
  public void readFrame(
      final ChannelHandlerContext ctx, final ByteBuf input, final Http2FrameListener listener)
      throws Http2Exception {
    reader.readFrame(
        ctx,
        input,
        new Http2FrameListener() {

          @Override
          public int onDataRead(
              final ChannelHandlerContext ctx,
              final int streamId,
              final ByteBuf data,
              final int padding,
              final boolean endOfStream)
              throws Http2Exception {
            logger.logData(INBOUND, ctx, streamId, data, padding, endOfStream);
            return listener.onDataRead(ctx, streamId, data, padding, endOfStream);
          }

          @Override
          public void onHeadersRead(
              final ChannelHandlerContext ctx,
              final int streamId,
              final Http2Headers headers,
              final int padding,
              final boolean endStream)
              throws Http2Exception {
            logger.logHeaders(INBOUND, ctx, streamId, headers, padding, endStream);
            listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
          }

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
              throws Http2Exception {
            logger.logHeaders(
                INBOUND,
                ctx,
                streamId,
                headers,
                streamDependency,
                weight,
                exclusive,
                padding,
                endStream);
            listener.onHeadersRead(
                ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
          }

          @Override
          public void onPriorityRead(
              final ChannelHandlerContext ctx,
              final int streamId,
              final int streamDependency,
              final short weight,
              final boolean exclusive)
              throws Http2Exception {
            logger.logPriority(INBOUND, ctx, streamId, streamDependency, weight, exclusive);
            listener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
          }

          @Override
          public void onRstStreamRead(
              final ChannelHandlerContext ctx, final int streamId, final long errorCode)
              throws Http2Exception {
            logger.logRstStream(INBOUND, ctx, streamId, errorCode);
            listener.onRstStreamRead(ctx, streamId, errorCode);
          }

          @Override
          public void onSettingsAckRead(final ChannelHandlerContext ctx) throws Http2Exception {
            logger.logSettingsAck(INBOUND, ctx);
            listener.onSettingsAckRead(ctx);
          }

          @Override
          public void onSettingsRead(final ChannelHandlerContext ctx, final Http2Settings settings)
              throws Http2Exception {
            logger.logSettings(INBOUND, ctx, settings);
            listener.onSettingsRead(ctx, settings);
          }

          @Override
          public void onPingRead(final ChannelHandlerContext ctx, final long data)
              throws Http2Exception {
            logger.logPing(INBOUND, ctx, data);
            listener.onPingRead(ctx, data);
          }

          @Override
          public void onPingAckRead(final ChannelHandlerContext ctx, final long data)
              throws Http2Exception {
            logger.logPingAck(INBOUND, ctx, data);
            listener.onPingAckRead(ctx, data);
          }

          @Override
          public void onPushPromiseRead(
              final ChannelHandlerContext ctx,
              final int streamId,
              final int promisedStreamId,
              final Http2Headers headers,
              final int padding)
              throws Http2Exception {
            logger.logPushPromise(INBOUND, ctx, streamId, promisedStreamId, headers, padding);
            listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
          }

          @Override
          public void onGoAwayRead(
              final ChannelHandlerContext ctx,
              final int lastStreamId,
              final long errorCode,
              final ByteBuf debugData)
              throws Http2Exception {
            logger.logGoAway(INBOUND, ctx, lastStreamId, errorCode, debugData);
            listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
          }

          @Override
          public void onWindowUpdateRead(
              final ChannelHandlerContext ctx, final int streamId, final int windowSizeIncrement)
              throws Http2Exception {
            logger.logWindowsUpdate(INBOUND, ctx, streamId, windowSizeIncrement);
            listener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
          }

          @Override
          public void onUnknownFrame(
              final ChannelHandlerContext ctx,
              final byte frameType,
              final int streamId,
              final Http2Flags flags,
              final ByteBuf payload)
              throws Http2Exception {
            logger.logUnknownFrame(INBOUND, ctx, frameType, streamId, flags, payload);
            listener.onUnknownFrame(ctx, frameType, streamId, flags, payload);
          }
        });
  }

  @Override
  public void close() {
    reader.close();
  }

  @Override
  public Configuration configuration() {
    return reader.configuration();
  }
}
