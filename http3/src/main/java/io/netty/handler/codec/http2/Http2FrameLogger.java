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

import static io.netty.util.internal.ObjectUtil.checkNotNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/** Logs HTTP2 frames for debugging purposes. */
@UnstableApi
public class Http2FrameLogger extends ChannelHandlerAdapter {

  public enum Direction {
    INBOUND,
    OUTBOUND
  }

  private static final int BUFFER_LENGTH_THRESHOLD = 64;
  private final InternalLogger logger;
  private final InternalLogLevel level;

  public Http2FrameLogger(final LogLevel level) {
    this(level.toInternalLevel(), InternalLoggerFactory.getInstance(Http2FrameLogger.class));
  }

  public Http2FrameLogger(final LogLevel level, final String name) {
    this(level.toInternalLevel(), InternalLoggerFactory.getInstance(name));
  }

  public Http2FrameLogger(final LogLevel level, final Class<?> clazz) {
    this(level.toInternalLevel(), InternalLoggerFactory.getInstance(clazz));
  }

  private Http2FrameLogger(final InternalLogLevel level, final InternalLogger logger) {
    this.level = checkNotNull(level, "level");
    this.logger = checkNotNull(logger, "logger");
  }

  public boolean isEnabled() {
    return logger.isEnabled(level);
  }

  public void logData(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int streamId,
      final ByteBuf data,
      final int padding,
      final boolean endStream) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} DATA: streamId={} padding={} endStream={} length={} bytes={}",
          ctx.channel(),
          direction.name(),
          streamId,
          padding,
          endStream,
          data.readableBytes(),
          toString(data));
    }
  }

  public void logHeaders(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int streamId,
      final Http2Headers headers,
      final int padding,
      final boolean endStream) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} HEADERS: streamId={} headers={} padding={} endStream={}",
          ctx.channel(),
          direction.name(),
          streamId,
          headers,
          padding,
          endStream);
    }
  }

  public void logHeaders(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int streamId,
      final Http2Headers headers,
      final int streamDependency,
      final short weight,
      final boolean exclusive,
      final int padding,
      final boolean endStream) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} HEADERS: streamId={} headers={} streamDependency={} weight={} exclusive={} "
              + "padding={} endStream={}",
          ctx.channel(),
          direction.name(),
          streamId,
          headers,
          streamDependency,
          weight,
          exclusive,
          padding,
          endStream);
    }
  }

  public void logPriority(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int streamId,
      final int streamDependency,
      final short weight,
      final boolean exclusive) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} PRIORITY: streamId={} streamDependency={} weight={} exclusive={}",
          ctx.channel(),
          direction.name(),
          streamId,
          streamDependency,
          weight,
          exclusive);
    }
  }

  public void logRstStream(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int streamId,
      final long errorCode) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} RST_STREAM: streamId={} errorCode={}",
          ctx.channel(),
          direction.name(),
          streamId,
          errorCode);
    }
  }

  public void logSettingsAck(final Direction direction, final ChannelHandlerContext ctx) {
    logger.log(level, "{} {} SETTINGS: ack=true", ctx.channel(), direction.name());
  }

  public void logSettings(
      final Direction direction, final ChannelHandlerContext ctx, final Http2Settings settings) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} SETTINGS: ack=false settings={}",
          ctx.channel(),
          direction.name(),
          settings);
    }
  }

  public void logPing(final Direction direction, final ChannelHandlerContext ctx, final long data) {
    if (isEnabled()) {
      logger.log(level, "{} {} PING: ack=false bytes={}", ctx.channel(), direction.name(), data);
    }
  }

  public void logPingAck(
      final Direction direction, final ChannelHandlerContext ctx, final long data) {
    if (isEnabled()) {
      logger.log(level, "{} {} PING: ack=true bytes={}", ctx.channel(), direction.name(), data);
    }
  }

  public void logPushPromise(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int streamId,
      final int promisedStreamId,
      final Http2Headers headers,
      final int padding) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} PUSH_PROMISE: streamId={} promisedStreamId={} headers={} padding={}",
          ctx.channel(),
          direction.name(),
          streamId,
          promisedStreamId,
          headers,
          padding);
    }
  }

  public void logGoAway(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int lastStreamId,
      final long errorCode,
      final ByteBuf debugData) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} GO_AWAY: lastStreamId={} errorCode={} length={} bytes={}",
          ctx.channel(),
          direction.name(),
          lastStreamId,
          errorCode,
          debugData.readableBytes(),
          toString(debugData));
    }
  }

  public void logWindowsUpdate(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final int streamId,
      final int windowSizeIncrement) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} WINDOW_UPDATE: streamId={} windowSizeIncrement={}",
          ctx.channel(),
          direction.name(),
          streamId,
          windowSizeIncrement);
    }
  }

  public void logUnknownFrame(
      final Direction direction,
      final ChannelHandlerContext ctx,
      final byte frameType,
      final int streamId,
      final Http2Flags flags,
      final ByteBuf data) {
    if (isEnabled()) {
      logger.log(
          level,
          "{} {} UNKNOWN: frameType={} streamId={} flags={} length={} bytes={}",
          ctx.channel(),
          direction.name(),
          frameType & 0xFF,
          streamId,
          flags.value(),
          data.readableBytes(),
          toString(data));
    }
  }

  private String toString(final ByteBuf buf) {
    if (level == InternalLogLevel.TRACE || buf.readableBytes() <= BUFFER_LENGTH_THRESHOLD) {
      // Log the entire buffer.
      return ByteBufUtil.hexDump(buf);
    }

    // Otherwise just log the first 64 bytes.
    final int length = Math.min(buf.readableBytes(), BUFFER_LENGTH_THRESHOLD);
    return ByteBufUtil.hexDump(buf, buf.readerIndex(), length) + "...";
  }
}
