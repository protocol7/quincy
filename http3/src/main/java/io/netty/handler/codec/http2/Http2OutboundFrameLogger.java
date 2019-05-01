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
package io.netty.handler.codec.http2;

import static io.netty.handler.codec.http2.Http2FrameLogger.Direction.OUTBOUND;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.UnstableApi;

/**
 * Decorator around a {@link Http2FrameWriter} that logs all outbound frames before calling the
 * writer.
 */
@UnstableApi
public class Http2OutboundFrameLogger implements Http2FrameWriter {
    private final Http2FrameWriter writer;
    private final Http2FrameLogger logger;

    public Http2OutboundFrameLogger(final Http2FrameWriter writer, final Http2FrameLogger logger) {
        this.writer = checkNotNull(writer, "writer");
        this.logger = checkNotNull(logger, "logger");
    }

    @Override
    public ChannelFuture writeData(final ChannelHandlerContext ctx, final int streamId, final ByteBuf data,
                                   final int padding, final boolean endStream, final ChannelPromise promise) {
        logger.logData(OUTBOUND, ctx, streamId, data, padding, endStream);
        return writer.writeData(ctx, streamId, data, padding, endStream, promise);
    }

    @Override
    public ChannelFuture writeHeaders(final ChannelHandlerContext ctx, final int streamId,
                                      final Http2Headers headers, final int padding, final boolean endStream, final ChannelPromise promise) {
        logger.logHeaders(OUTBOUND, ctx, streamId, headers, padding, endStream);
        return writer.writeHeaders(ctx, streamId, headers, padding, endStream, promise);
    }

    @Override
    public ChannelFuture writeHeaders(final ChannelHandlerContext ctx, final int streamId,
                                      final Http2Headers headers, final int streamDependency, final short weight, final boolean exclusive,
                                      final int padding, final boolean endStream, final ChannelPromise promise) {
        logger.logHeaders(OUTBOUND, ctx, streamId, headers, streamDependency, weight, exclusive,
                padding, endStream);
        return writer.writeHeaders(ctx, streamId, headers, streamDependency, weight,
                exclusive, padding, endStream, promise);
    }

    @Override
    public ChannelFuture writePriority(final ChannelHandlerContext ctx, final int streamId,
                                       final int streamDependency, final short weight, final boolean exclusive, final ChannelPromise promise) {
        logger.logPriority(OUTBOUND, ctx, streamId, streamDependency, weight, exclusive);
        return writer.writePriority(ctx, streamId, streamDependency, weight, exclusive, promise);
    }

    @Override
    public ChannelFuture writeRstStream(final ChannelHandlerContext ctx,
                                        final int streamId, final long errorCode, final ChannelPromise promise) {
        logger.logRstStream(OUTBOUND, ctx, streamId, errorCode);
        return writer.writeRstStream(ctx, streamId, errorCode, promise);
    }

    @Override
    public ChannelFuture writeSettings(final ChannelHandlerContext ctx,
                                       final Http2Settings settings, final ChannelPromise promise) {
        logger.logSettings(OUTBOUND, ctx, settings);
        return writer.writeSettings(ctx, settings, promise);
    }

    @Override
    public ChannelFuture writeSettingsAck(final ChannelHandlerContext ctx, final ChannelPromise promise) {
        logger.logSettingsAck(OUTBOUND, ctx);
        return writer.writeSettingsAck(ctx, promise);
    }

    @Override
    public ChannelFuture writePing(final ChannelHandlerContext ctx, final boolean ack,
                                   final long data, final ChannelPromise promise) {
        if (ack) {
            logger.logPingAck(OUTBOUND, ctx, data);
        } else {
            logger.logPing(OUTBOUND, ctx, data);
        }
        return writer.writePing(ctx, ack, data, promise);
    }

    @Override
    public ChannelFuture writePushPromise(final ChannelHandlerContext ctx, final int streamId,
                                          final int promisedStreamId, final Http2Headers headers, final int padding, final ChannelPromise promise) {
        logger.logPushPromise(OUTBOUND, ctx, streamId, promisedStreamId, headers, padding);
        return writer.writePushPromise(ctx, streamId, promisedStreamId, headers, padding, promise);
    }

    @Override
    public ChannelFuture writeGoAway(final ChannelHandlerContext ctx, final int lastStreamId, final long errorCode,
                                     final ByteBuf debugData, final ChannelPromise promise) {
        logger.logGoAway(OUTBOUND, ctx, lastStreamId, errorCode, debugData);
        return writer.writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
    }

    @Override
    public ChannelFuture writeWindowUpdate(final ChannelHandlerContext ctx,
                                           final int streamId, final int windowSizeIncrement, final ChannelPromise promise) {
        logger.logWindowsUpdate(OUTBOUND, ctx, streamId, windowSizeIncrement);
        return writer.writeWindowUpdate(ctx, streamId, windowSizeIncrement, promise);
    }

    @Override
    public ChannelFuture writeFrame(final ChannelHandlerContext ctx, final byte frameType, final int streamId,
                                    final Http2Flags flags, final ByteBuf payload, final ChannelPromise promise) {
        logger.logUnknownFrame(OUTBOUND, ctx, frameType, streamId, flags, payload);
        return writer.writeFrame(ctx, frameType, streamId, flags, payload, promise);
    }

    @Override
    public void close() {
        writer.close();
    }

    @Override
    public Configuration configuration() {
        return writer.configuration();
    }
}
