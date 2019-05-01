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

import static io.netty.buffer.ByteBufUtil.hexDump;
import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.util.CharsetUtil.UTF_8;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static java.lang.Math.min;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Provides the default implementation for processing inbound frame events and delegates to a {@link
 * Http2FrameListener}
 *
 * <p>This class will read HTTP/2 frames and delegate the events to a {@link Http2FrameListener}
 *
 * <p>This interface enforces inbound flow control functionality through {@link
 * Http2LocalFlowController}
 */
@UnstableApi
public class Http2ConnectionHandler extends ByteToMessageDecoder
    implements Http2LifecycleManager, ChannelOutboundHandler {

  private static final InternalLogger logger =
      InternalLoggerFactory.getInstance(Http2ConnectionHandler.class);

  private static final Http2Headers HEADERS_TOO_LARGE_HEADERS =
      ReadOnlyHttp2Headers.serverHeaders(
          false, HttpResponseStatus.REQUEST_HEADER_FIELDS_TOO_LARGE.codeAsText());
  private static final ByteBuf HTTP_1_X_BUF =
      Unpooled.unreleasableBuffer(
              Unpooled.wrappedBuffer(new byte[] {'H', 'T', 'T', 'P', '/', '1', '.'}))
          .asReadOnly();

  private final Http2ConnectionDecoder decoder;
  private final Http2ConnectionEncoder encoder;
  private final Http2Settings initialSettings;
  private final boolean decoupleCloseAndGoAway;
  private ChannelFutureListener closeListener;
  private BaseDecoder byteDecoder;
  private long gracefulShutdownTimeoutMillis;

  protected Http2ConnectionHandler(
      final Http2ConnectionDecoder decoder,
      final Http2ConnectionEncoder encoder,
      final Http2Settings initialSettings) {
    this(decoder, encoder, initialSettings, false);
  }

  protected Http2ConnectionHandler(
      final Http2ConnectionDecoder decoder,
      final Http2ConnectionEncoder encoder,
      final Http2Settings initialSettings,
      final boolean decoupleCloseAndGoAway) {
    this.initialSettings = checkNotNull(initialSettings, "initialSettings");
    this.decoder = checkNotNull(decoder, "decoder");
    this.encoder = checkNotNull(encoder, "encoder");
    this.decoupleCloseAndGoAway = decoupleCloseAndGoAway;
    if (encoder.connection() != decoder.connection()) {
      throw new IllegalArgumentException(
          "Encoder and Decoder do not share the same connection object");
    }
  }

  /**
   * Get the amount of time (in milliseconds) this endpoint will wait for all streams to be closed
   * before closing the connection during the graceful shutdown process. Returns -1 if this
   * connection is configured to wait indefinitely for all streams to close.
   */
  public long gracefulShutdownTimeoutMillis() {
    return gracefulShutdownTimeoutMillis;
  }

  /**
   * Set the amount of time (in milliseconds) this endpoint will wait for all streams to be closed
   * before closing the connection during the graceful shutdown process.
   *
   * @param gracefulShutdownTimeoutMillis the amount of time (in milliseconds) this endpoint will
   *     wait for all streams to be closed before closing the connection during the graceful
   *     shutdown process.
   */
  public void gracefulShutdownTimeoutMillis(final long gracefulShutdownTimeoutMillis) {
    if (gracefulShutdownTimeoutMillis < -1) {
      throw new IllegalArgumentException(
          "gracefulShutdownTimeoutMillis: "
              + gracefulShutdownTimeoutMillis
              + " (expected: -1 for indefinite or >= 0)");
    }
    this.gracefulShutdownTimeoutMillis = gracefulShutdownTimeoutMillis;
  }

  public Http2Connection connection() {
    return encoder.connection();
  }

  public Http2ConnectionDecoder decoder() {
    return decoder;
  }

  public Http2ConnectionEncoder encoder() {
    return encoder;
  }

  private boolean prefaceSent() {
    return byteDecoder != null && byteDecoder.prefaceSent();
  }

  /**
   * Handles the client-side (cleartext) upgrade from HTTP to HTTP/2. Reserves local stream 1 for
   * the HTTP/2 response.
   */
  public void onHttpClientUpgrade() throws Http2Exception {
    if (connection().isServer()) {
      throw Http2Exception.connectionError(
          Http2Error.PROTOCOL_ERROR, "Client-side HTTP upgrade requested for a server");
    }
    if (!prefaceSent()) {
      // If the preface was not sent yet it most likely means the handler was not added to the
      // pipeline before
      // calling this method.
      throw Http2Exception.connectionError(
          Http2Error.INTERNAL_ERROR, "HTTP upgrade must occur after preface was sent");
    }
    if (decoder.prefaceReceived()) {
      throw Http2Exception.connectionError(
          Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is received");
    }

    // Create a local stream used for the HTTP cleartext upgrade.
    connection().local().createStream(Http2CodecUtil.HTTP_UPGRADE_STREAM_ID, true);
  }

  /**
   * Handles the server-side (cleartext) upgrade from HTTP to HTTP/2.
   *
   * @param settings the settings for the remote endpoint.
   */
  public void onHttpServerUpgrade(final Http2Settings settings) throws Http2Exception {
    if (!connection().isServer()) {
      throw Http2Exception.connectionError(
          Http2Error.PROTOCOL_ERROR, "Server-side HTTP upgrade requested for a client");
    }
    if (!prefaceSent()) {
      // If the preface was not sent yet it most likely means the handler was not added to the
      // pipeline before
      // calling this method.
      throw Http2Exception.connectionError(
          Http2Error.INTERNAL_ERROR, "HTTP upgrade must occur after preface was sent");
    }
    if (decoder.prefaceReceived()) {
      throw Http2Exception.connectionError(
          Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is received");
    }

    // Apply the settings but no ACK is necessary.
    encoder.remoteSettings(settings);

    // Create a stream in the half-closed state.
    connection().remote().createStream(Http2CodecUtil.HTTP_UPGRADE_STREAM_ID, true);
  }

  @Override
  public void flush(final ChannelHandlerContext ctx) {
    try {
      // Trigger pending writes in the remote flow controller.
      encoder.flowController().writePendingBytes();
      ctx.flush();
    } catch (final Http2Exception e) {
      onError(ctx, true, e);
    } catch (final Throwable cause) {
      onError(
          ctx,
          true,
          Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, cause, "Error flushing"));
    }
  }

  private abstract class BaseDecoder {
    public abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
        throws Exception;

    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {}

    public void channelActive(final ChannelHandlerContext ctx) throws Exception {}

    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
      // Connection has terminated, close the encoder and decoder.
      encoder().close();
      decoder().close();

      // We need to remove all streams (not just the active ones).
      // See https://github.com/netty/netty/issues/4838.
      connection().close(ctx.voidPromise());
    }

    /** Determine if the HTTP/2 connection preface been sent. */
    public boolean prefaceSent() {
      return true;
    }
  }

  private final class PrefaceDecoder extends BaseDecoder {
    private ByteBuf clientPrefaceString;
    private boolean prefaceSent;

    PrefaceDecoder(final ChannelHandlerContext ctx) throws Exception {
      clientPrefaceString = clientPrefaceString(encoder.connection());
      // This handler was just added to the context. In case it was handled after
      // the connection became active, send the connection preface now.
      sendPreface(ctx);
    }

    @Override
    public boolean prefaceSent() {
      return prefaceSent;
    }

    @Override
    public void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
        throws Exception {
      try {
        if (ctx.channel().isActive()
            && readClientPrefaceString(in)
            && verifyFirstFrameIsSettings(in)) {
          // After the preface is read, it is time to hand over control to the post initialized
          // decoder.
          byteDecoder = new FrameDecoder();
          byteDecoder.decode(ctx, in, out);
        }
      } catch (final Throwable e) {
        onError(ctx, false, e);
      }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
      // The channel just became active - send the connection preface to the remote endpoint.
      sendPreface(ctx);
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
      cleanup();
      super.channelInactive(ctx);
    }

    /** Releases the {@code clientPrefaceString}. Any active streams will be left in the open. */
    @Override
    public void handlerRemoved(final ChannelHandlerContext ctx) throws Exception {
      cleanup();
    }

    /** Releases the {@code clientPrefaceString}. Any active streams will be left in the open. */
    private void cleanup() {
      if (clientPrefaceString != null) {
        clientPrefaceString.release();
        clientPrefaceString = null;
      }
    }

    /**
     * Decodes the client connection preface string from the input buffer.
     *
     * @return {@code true} if processing of the client preface string is complete. Since client
     *     preface strings can only be received by servers, returns true immediately for client
     *     endpoints.
     */
    private boolean readClientPrefaceString(final ByteBuf in) throws Http2Exception {
      if (clientPrefaceString == null) {
        return true;
      }

      final int prefaceRemaining = clientPrefaceString.readableBytes();
      final int bytesRead = min(in.readableBytes(), prefaceRemaining);

      // If the input so far doesn't match the preface, break the connection.
      if (bytesRead == 0
          || !ByteBufUtil.equals(
              in,
              in.readerIndex(),
              clientPrefaceString,
              clientPrefaceString.readerIndex(),
              bytesRead)) {
        final int maxSearch = 1024; // picked because 512 is too little, and 2048 too much
        final int http1Index =
            ByteBufUtil.indexOf(
                HTTP_1_X_BUF, in.slice(in.readerIndex(), min(in.readableBytes(), maxSearch)));
        if (http1Index != -1) {
          final String chunk =
              in.toString(in.readerIndex(), http1Index - in.readerIndex(), CharsetUtil.US_ASCII);
          throw Http2Exception.connectionError(
              Http2Error.PROTOCOL_ERROR, "Unexpected HTTP/1.x request: %s", chunk);
        }
        final String receivedBytes =
            hexDump(
                in, in.readerIndex(), min(in.readableBytes(), clientPrefaceString.readableBytes()));
        throw Http2Exception.connectionError(
            Http2Error.PROTOCOL_ERROR,
            "HTTP/2 client preface string missing or corrupt. " + "Hex dump for received bytes: %s",
            receivedBytes);
      }
      in.skipBytes(bytesRead);
      clientPrefaceString.skipBytes(bytesRead);

      if (!clientPrefaceString.isReadable()) {
        // Entire preface has been read.
        clientPrefaceString.release();
        clientPrefaceString = null;
        return true;
      }
      return false;
    }

    /**
     * Peeks at that the next frame in the buffer and verifies that it is a non-ack {@code SETTINGS}
     * frame.
     *
     * @param in the inbound buffer.
     * @return {@code} true if the next frame is a non-ack {@code SETTINGS} frame, {@code false} if
     *     more data is required before we can determine the next frame type.
     * @throws Http2Exception thrown if the next frame is NOT a non-ack {@code SETTINGS} frame.
     */
    private boolean verifyFirstFrameIsSettings(final ByteBuf in) throws Http2Exception {
      if (in.readableBytes() < 5) {
        // Need more data before we can see the frame type for the first frame.
        return false;
      }

      final short frameType = in.getUnsignedByte(in.readerIndex() + 3);
      final short flags = in.getUnsignedByte(in.readerIndex() + 4);
      if (frameType != Http2FrameTypes.SETTINGS || (flags & Http2Flags.ACK) != 0) {
        throw Http2Exception.connectionError(
            Http2Error.PROTOCOL_ERROR,
            "First received frame was not SETTINGS. " + "Hex dump for first 5 bytes: %s",
            hexDump(in, in.readerIndex(), 5));
      }
      return true;
    }

    /**
     * Sends the HTTP/2 connection preface upon establishment of the connection, if not already
     * sent.
     */
    private void sendPreface(final ChannelHandlerContext ctx) throws Exception {
      if (prefaceSent || !ctx.channel().isActive()) {
        return;
      }

      prefaceSent = true;

      final boolean isClient = !connection().isServer();
      if (isClient) {
        // Clients must send the preface string as the first bytes on the connection.
        ctx.write(Http2CodecUtil.connectionPrefaceBuf())
            .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      }

      // Both client and server must send their initial settings.
      encoder
          .writeSettings(ctx, initialSettings, ctx.newPromise())
          .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

      if (isClient) {
        // If this handler is extended by the user and we directly fire the userEvent from this
        // context then
        // the user will not see the event. We should fire the event starting with this handler so
        // this class
        // (and extending classes) have a chance to process the event.
        userEventTriggered(ctx, Http2ConnectionPrefaceAndSettingsFrameWrittenEvent.INSTANCE);
      }
    }
  }

  private final class FrameDecoder extends BaseDecoder {
    @Override
    public void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
        throws Exception {
      try {
        decoder.decodeFrame(ctx, in, out);
      } catch (final Throwable e) {
        onError(ctx, false, e);
      }
    }
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
    // Initialize the encoder, decoder, flow controllers, and internal state.
    encoder.lifecycleManager(this);
    decoder.lifecycleManager(this);
    encoder.flowController().channelHandlerContext(ctx);
    decoder.flowController().channelHandlerContext(ctx);
    byteDecoder = new PrefaceDecoder(ctx);
  }

  @Override
  protected void handlerRemoved0(final ChannelHandlerContext ctx) throws Exception {
    if (byteDecoder != null) {
      byteDecoder.handlerRemoved(ctx);
      byteDecoder = null;
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    if (byteDecoder == null) {
      byteDecoder = new PrefaceDecoder(ctx);
    }
    byteDecoder.channelActive(ctx);
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
    // Call super class first, as this may result in decode being called.
    super.channelInactive(ctx);
    if (byteDecoder != null) {
      byteDecoder.channelInactive(ctx);
      byteDecoder = null;
    }
  }

  @Override
  public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
    // Writability is expected to change while we are writing. We cannot allow this event to trigger
    // reentering
    // the allocation and write loop. Reentering the event loop will lead to over or illegal
    // allocation.
    try {
      if (ctx.channel().isWritable()) {
        flush(ctx);
      }
      encoder.flowController().channelWritabilityChanged();
    } finally {
      super.channelWritabilityChanged(ctx);
    }
  }

  @Override
  protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
      throws Exception {
    byteDecoder.decode(ctx, in, out);
  }

  @Override
  public void bind(
      final ChannelHandlerContext ctx,
      final SocketAddress localAddress,
      final ChannelPromise promise)
      throws Exception {
    ctx.bind(localAddress, promise);
  }

  @Override
  public void connect(
      final ChannelHandlerContext ctx,
      final SocketAddress remoteAddress,
      final SocketAddress localAddress,
      final ChannelPromise promise)
      throws Exception {
    ctx.connect(remoteAddress, localAddress, promise);
  }

  @Override
  public void disconnect(final ChannelHandlerContext ctx, final ChannelPromise promise)
      throws Exception {
    ctx.disconnect(promise);
  }

  @Override
  public void close(
      final ChannelHandlerContext ctx,
      @SuppressWarnings("checkstyle:finalparameters") ChannelPromise promise)
      throws Exception {
    if (decoupleCloseAndGoAway) {
      ctx.close(promise);
      return;
    }
    promise = promise.unvoid();
    // Avoid NotYetConnectedException
    if (!ctx.channel().isActive()) {
      ctx.close(promise);
      return;
    }

    // If the user has already sent a GO_AWAY frame they may be attempting to do a graceful shutdown
    // which requires
    // sending multiple GO_AWAY frames. We should only send a GO_AWAY here if one has not already
    // been sent. If
    // a GO_AWAY has been sent we send a empty buffer just so we can wait to close until all other
    // data has been
    // flushed to the OS.
    // https://github.com/netty/netty/issues/5307
    final ChannelFuture f =
        connection().goAwaySent() ? ctx.write(EMPTY_BUFFER) : goAway(ctx, null, ctx.newPromise());
    ctx.flush();
    doGracefulShutdown(ctx, f, promise);
  }

  private void doGracefulShutdown(
      final ChannelHandlerContext ctx, final ChannelFuture future, final ChannelPromise promise) {
    if (isGracefulShutdownComplete()) {
      // If there are no active streams, close immediately after the GO_AWAY write completes.
      future.addListener(new ClosingChannelFutureListener(ctx, promise));
    } else {
      // If there are active streams we should wait until they are all closed before closing the
      // connection.
      final ClosingChannelFutureListener tmp =
          gracefulShutdownTimeoutMillis < 0
              ? new ClosingChannelFutureListener(ctx, promise)
              : new ClosingChannelFutureListener(
                  ctx, promise, gracefulShutdownTimeoutMillis, MILLISECONDS);
      // The ClosingChannelFutureListener will cascade promise completion. We need to always notify
      // the
      // new ClosingChannelFutureListener when the graceful close completes if the promise is not
      // null.
      if (closeListener == null) {
        closeListener = tmp;
      } else if (promise != null) {
        final ChannelFutureListener oldCloseListener = closeListener;
        closeListener =
            new ChannelFutureListener() {
              @Override
              public void operationComplete(final ChannelFuture future) throws Exception {
                try {
                  oldCloseListener.operationComplete(future);
                } finally {
                  tmp.operationComplete(future);
                }
              }
            };
      }
    }
  }

  @Override
  public void deregister(final ChannelHandlerContext ctx, final ChannelPromise promise)
      throws Exception {
    ctx.deregister(promise);
  }

  @Override
  public void read(final ChannelHandlerContext ctx) throws Exception {
    ctx.read();
  }

  @Override
  public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise)
      throws Exception {
    ctx.write(msg, promise);
  }

  @Override
  public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
    // Trigger flush after read on the assumption that flush is cheap if there is nothing to write
    // and that
    // for flow-control the read may release window that causes data to be written that can now be
    // flushed.
    try {
      // First call channelReadComplete0(...) as this may produce more data that we want to flush
      channelReadComplete0(ctx);
    } finally {
      flush(ctx);
    }
  }

  final void channelReadComplete0(final ChannelHandlerContext ctx) {
    // Discard bytes of the cumulation buffer if needed.
    discardSomeReadBytes();

    // Ensure we never stale the HTTP/2 Channel. Flow-control is enforced by HTTP/2.
    //
    // See https://tools.ietf.org/html/rfc7540#section-5.2.2
    if (!ctx.channel().config().isAutoRead()) {
      ctx.read();
    }

    ctx.fireChannelReadComplete();
  }

  /**
   * Handles {@link Http2Exception} objects that were thrown from other handlers. Ignores all other
   * exceptions.
   */
  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
      throws Exception {
    if (Http2CodecUtil.getEmbeddedHttp2Exception(cause) != null) {
      // Some exception in the causality chain is an Http2Exception - handle it.
      onError(ctx, false, cause);
    } else {
      super.exceptionCaught(ctx, cause);
    }
  }

  /**
   * Closes the local side of the given stream. If this causes the stream to be closed, adds a hook
   * to close the channel after the given future completes.
   *
   * @param stream the stream to be half closed.
   * @param future If closing, the future after which to close the channel.
   */
  @Override
  public void closeStreamLocal(final Http2Stream stream, final ChannelFuture future) {
    switch (stream.state()) {
      case HALF_CLOSED_LOCAL:
      case OPEN:
        stream.closeLocalSide();
        break;
      default:
        closeStream(stream, future);
        break;
    }
  }

  /**
   * Closes the remote side of the given stream. If this causes the stream to be closed, adds a hook
   * to close the channel after the given future completes.
   *
   * @param stream the stream to be half closed.
   * @param future If closing, the future after which to close the channel.
   */
  @Override
  public void closeStreamRemote(final Http2Stream stream, final ChannelFuture future) {
    switch (stream.state()) {
      case HALF_CLOSED_REMOTE:
      case OPEN:
        stream.closeRemoteSide();
        break;
      default:
        closeStream(stream, future);
        break;
    }
  }

  @Override
  public void closeStream(final Http2Stream stream, final ChannelFuture future) {
    stream.close();

    if (future.isDone()) {
      checkCloseConnection(future);
    } else {
      future.addListener(
          new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
              checkCloseConnection(future);
            }
          });
    }
  }

  /** Central handler for all exceptions caught during HTTP/2 processing. */
  @Override
  public void onError(
      final ChannelHandlerContext ctx, final boolean outbound, final Throwable cause) {
    final Http2Exception embedded = Http2CodecUtil.getEmbeddedHttp2Exception(cause);
    if (Http2Exception.isStreamError(embedded)) {
      onStreamError(ctx, outbound, cause, (Http2Exception.StreamException) embedded);
    } else if (embedded instanceof Http2Exception.CompositeStreamException) {
      final Http2Exception.CompositeStreamException compositException =
          (Http2Exception.CompositeStreamException) embedded;
      for (final Http2Exception.StreamException streamException : compositException) {
        onStreamError(ctx, outbound, cause, streamException);
      }
    } else {
      onConnectionError(ctx, outbound, cause, embedded);
    }
    ctx.flush();
  }

  /**
   * Called by the graceful shutdown logic to determine when it is safe to close the connection.
   * Returns {@code true} if the graceful shutdown has completed and the connection can be safely
   * closed. This implementation just guarantees that there are no active streams. Subclasses may
   * override to provide additional checks.
   */
  protected boolean isGracefulShutdownComplete() {
    return connection().numActiveStreams() == 0;
  }

  /**
   * Handler for a connection error. Sends a GO_AWAY frame to the remote endpoint. Once all streams
   * are closed, the connection is shut down.
   *
   * @param ctx the channel context
   * @param outbound {@code true} if the error was caused by an outbound operation.
   * @param cause the exception that was caught
   * @param http2Ex the {@link Http2Exception} that is embedded in the causality chain. This may be
   *     {@code null} if it's an unknown exception.
   */
  protected void onConnectionError(
      final ChannelHandlerContext ctx,
      final boolean outbound,
      final Throwable cause,
      Http2Exception http2Ex) {
    if (http2Ex == null) {
      http2Ex = new Http2Exception(Http2Error.INTERNAL_ERROR, cause.getMessage(), cause);
    }

    final ChannelPromise promise = ctx.newPromise();
    final ChannelFuture future = goAway(ctx, http2Ex, ctx.newPromise());
    if (http2Ex.shutdownHint() == Http2Exception.ShutdownHint.GRACEFUL_SHUTDOWN) {
      doGracefulShutdown(ctx, future, promise);
    } else {
      future.addListener(new ClosingChannelFutureListener(ctx, promise));
    }
  }

  /**
   * Handler for a stream error. Sends a {@code RST_STREAM} frame to the remote endpoint and closes
   * the stream.
   *
   * @param ctx the channel context
   * @param outbound {@code true} if the error was caused by an outbound operation.
   * @param cause the exception that was caught
   * @param http2Ex the {@link Http2Exception.StreamException} that is embedded in the causality
   *     chain.
   */
  protected void onStreamError(
      final ChannelHandlerContext ctx,
      final boolean outbound,
      @SuppressWarnings("unused") final Throwable cause,
      final Http2Exception.StreamException http2Ex) {
    final int streamId = http2Ex.streamId();
    Http2Stream stream = connection().stream(streamId);

    // if this is caused by reading headers that are too large, send a header with status 431
    if (http2Ex instanceof Http2Exception.HeaderListSizeException
        && ((Http2Exception.HeaderListSizeException) http2Ex).duringDecode()
        && connection().isServer()) {

      // NOTE We have to check to make sure that a stream exists before we send our reply.
      // We likely always create the stream below as the stream isn't created until the
      // header block is completely processed.

      // The case of a streamId referring to a stream which was already closed is handled
      // by createStream and will land us in the catch block below
      if (stream == null) {
        try {
          stream = encoder.connection().remote().createStream(streamId, true);
        } catch (final Http2Exception e) {
          resetUnknownStream(ctx, streamId, http2Ex.error().code(), ctx.newPromise());
          return;
        }
      }

      // ensure that we have not already sent headers on this stream
      if (stream != null && !stream.isHeadersSent()) {
        try {
          handleServerHeaderDecodeSizeError(ctx, stream);
        } catch (final Throwable cause2) {
          onError(
              ctx,
              outbound,
              Http2Exception.connectionError(
                  Http2Error.INTERNAL_ERROR, cause2, "Error DecodeSizeError"));
        }
      }
    }

    if (stream == null) {
      if (!outbound || connection().local().mayHaveCreatedStream(streamId)) {
        resetUnknownStream(ctx, streamId, http2Ex.error().code(), ctx.newPromise());
      }
    } else {
      resetStream(ctx, stream, http2Ex.error().code(), ctx.newPromise());
    }
  }

  /**
   * Notifies client that this server has received headers that are larger than what it is willing
   * to accept. Override to change behavior.
   *
   * @param ctx the channel context
   * @param stream the Http2Stream on which the header was received
   */
  protected void handleServerHeaderDecodeSizeError(
      final ChannelHandlerContext ctx, final Http2Stream stream) {
    encoder().writeHeaders(ctx, stream.id(), HEADERS_TOO_LARGE_HEADERS, 0, true, ctx.newPromise());
  }

  protected Http2FrameWriter frameWriter() {
    return encoder().frameWriter();
  }

  /**
   * Sends a {@code RST_STREAM} frame even if we don't know about the stream. This error condition
   * is most likely triggered by the first frame of a stream being invalid. That is, there was an
   * error reading the frame before we could create a new stream.
   */
  private ChannelFuture resetUnknownStream(
      final ChannelHandlerContext ctx,
      final int streamId,
      final long errorCode,
      final ChannelPromise promise) {
    final ChannelFuture future = frameWriter().writeRstStream(ctx, streamId, errorCode, promise);
    if (future.isDone()) {
      closeConnectionOnError(ctx, future);
    } else {
      future.addListener(
          new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
              closeConnectionOnError(ctx, future);
            }
          });
    }
    return future;
  }

  @Override
  public ChannelFuture resetStream(
      final ChannelHandlerContext ctx,
      final int streamId,
      final long errorCode,
      final ChannelPromise promise) {
    final Http2Stream stream = connection().stream(streamId);
    if (stream == null) {
      return resetUnknownStream(ctx, streamId, errorCode, promise.unvoid());
    }

    return resetStream(ctx, stream, errorCode, promise);
  }

  private ChannelFuture resetStream(
      final ChannelHandlerContext ctx,
      final Http2Stream stream,
      final long errorCode,
      ChannelPromise promise) {
    promise = promise.unvoid();
    if (stream.isResetSent()) {
      // Don't write a RST_STREAM frame if we have already written one.
      return promise.setSuccess();
    }
    final ChannelFuture future;
    // If the remote peer is not aware of the steam, then we are not allowed to send a RST_STREAM
    // https://tools.ietf.org/html/rfc7540#section-6.4.
    if (stream.state() == Http2Stream.State.IDLE
        || connection().local().created(stream)
            && !stream.isHeadersSent()
            && !stream.isPushPromiseSent()) {
      future = promise.setSuccess();
    } else {
      future = frameWriter().writeRstStream(ctx, stream.id(), errorCode, promise);
    }

    // Synchronously set the resetSent flag to prevent any subsequent calls
    // from resulting in multiple reset frames being sent.
    stream.resetSent();

    if (future.isDone()) {
      processRstStreamWriteResult(ctx, stream, future);
    } else {
      future.addListener(
          new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
              processRstStreamWriteResult(ctx, stream, future);
            }
          });
    }

    return future;
  }

  @Override
  public ChannelFuture goAway(
      final ChannelHandlerContext ctx,
      final int lastStreamId,
      final long errorCode,
      final ByteBuf debugData,
      ChannelPromise promise) {
    promise = promise.unvoid();
    final Http2Connection connection = connection();
    try {
      if (!connection.goAwaySent(lastStreamId, errorCode, debugData)) {
        debugData.release();
        promise.trySuccess();
        return promise;
      }
    } catch (final Throwable cause) {
      debugData.release();
      promise.tryFailure(cause);
      return promise;
    }

    // Need to retain before we write the buffer because if we do it after the refCnt could already
    // be 0 and
    // result in an IllegalRefCountException.
    debugData.retain();
    final ChannelFuture future =
        frameWriter().writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);

    if (future.isDone()) {
      processGoAwayWriteResult(ctx, lastStreamId, errorCode, debugData, future);
    } else {
      future.addListener(
          new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
              processGoAwayWriteResult(ctx, lastStreamId, errorCode, debugData, future);
            }
          });
    }
    // if closeListener != null this means we have already initiated graceful closure.
    // doGracefulShutdown will apply
    // the gracefulShutdownTimeoutMillis on each invocation, however we only care to apply the
    // timeout on the
    // start of graceful shutdown.
    if (errorCode == Http2Error.NO_ERROR.code() && closeListener == null) {
      doGracefulShutdown(ctx, future, null);
    }

    return future;
  }

  /**
   * Closes the connection if the graceful shutdown process has completed.
   *
   * @param future Represents the status that will be passed to the {@link #closeListener}.
   */
  private void checkCloseConnection(final ChannelFuture future) {
    // If this connection is closing and the graceful shutdown has completed, close the connection
    // once this operation completes.
    if (closeListener != null && isGracefulShutdownComplete()) {
      final ChannelFutureListener closeListener = this.closeListener;
      // This method could be called multiple times
      // and we don't want to notify the closeListener multiple times.
      this.closeListener = null;
      try {
        closeListener.operationComplete(future);
      } catch (final Exception e) {
        throw new IllegalStateException("Close listener threw an unexpected exception", e);
      }
    }
  }

  /**
   * Close the remote endpoint with with a {@code GO_AWAY} frame. Does <strong>not</strong> flush
   * immediately, this is the responsibility of the caller.
   */
  private ChannelFuture goAway(
      final ChannelHandlerContext ctx, final Http2Exception cause, final ChannelPromise promise) {
    final long errorCode = cause != null ? cause.error().code() : Http2Error.NO_ERROR.code();
    final int lastKnownStream = connection().remote().lastStreamCreated();
    return goAway(ctx, lastKnownStream, errorCode, Http2CodecUtil.toByteBuf(ctx, cause), promise);
  }

  private void processRstStreamWriteResult(
      final ChannelHandlerContext ctx, final Http2Stream stream, final ChannelFuture future) {
    if (future.isSuccess()) {
      closeStream(stream, future);
    } else {
      // The connection will be closed and so no need to change the resetSent flag to false.
      onConnectionError(ctx, true, future.cause(), null);
    }
  }

  private void closeConnectionOnError(final ChannelHandlerContext ctx, final ChannelFuture future) {
    if (!future.isSuccess()) {
      onConnectionError(ctx, true, future.cause(), null);
    }
  }

  /**
   * Returns the client preface string if this is a client connection, otherwise returns {@code
   * null}.
   */
  private static ByteBuf clientPrefaceString(final Http2Connection connection) {
    return connection.isServer() ? Http2CodecUtil.connectionPrefaceBuf() : null;
  }

  private static void processGoAwayWriteResult(
      final ChannelHandlerContext ctx,
      final int lastStreamId,
      final long errorCode,
      final ByteBuf debugData,
      final ChannelFuture future) {
    try {
      if (future.isSuccess()) {
        if (errorCode != Http2Error.NO_ERROR.code()) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "{} Sent GOAWAY: lastStreamId '{}', errorCode '{}', "
                    + "debugData '{}'. Forcing shutdown of the connection.",
                ctx.channel(),
                lastStreamId,
                errorCode,
                debugData.toString(UTF_8),
                future.cause());
          }
          ctx.close();
        }
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug(
              "{} Sending GOAWAY failed: lastStreamId '{}', errorCode '{}', "
                  + "debugData '{}'. Forcing shutdown of the connection.",
              ctx.channel(),
              lastStreamId,
              errorCode,
              debugData.toString(UTF_8),
              future.cause());
        }
        ctx.close();
      }
    } finally {
      // We're done with the debug data now.
      debugData.release();
    }
  }

  /** Closes the channel when the future completes. */
  private static final class ClosingChannelFutureListener implements ChannelFutureListener {
    private final ChannelHandlerContext ctx;
    private final ChannelPromise promise;
    private final ScheduledFuture<?> timeoutTask;

    ClosingChannelFutureListener(final ChannelHandlerContext ctx, final ChannelPromise promise) {
      this.ctx = ctx;
      this.promise = promise;
      timeoutTask = null;
    }

    ClosingChannelFutureListener(
        final ChannelHandlerContext ctx,
        final ChannelPromise promise,
        final long timeout,
        final TimeUnit unit) {
      this.ctx = ctx;
      this.promise = promise;
      timeoutTask =
          ctx.executor()
              .schedule(
                  new Runnable() {
                    @Override
                    public void run() {
                      doClose();
                    }
                  },
                  timeout,
                  unit);
    }

    @Override
    public void operationComplete(final ChannelFuture sentGoAwayFuture) {
      if (timeoutTask != null) {
        timeoutTask.cancel(false);
      }
      doClose();
    }

    private void doClose() {
      if (promise == null) {
        ctx.close();
      } else {
        ctx.close(promise);
      }
    }
  }
}
