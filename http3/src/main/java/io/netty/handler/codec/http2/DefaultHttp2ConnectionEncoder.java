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
package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CoalescingBufferQueue;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.handler.codec.http2.Http2CodecUtil.SimpleChannelPromiseAggregator;
import io.netty.util.internal.UnstableApi;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.netty.handler.codec.http.HttpStatusClass.INFORMATIONAL;
import static io.netty.handler.codec.http2.Http2CodecUtil.DEFAULT_PRIORITY_WEIGHT;
import static io.netty.handler.codec.http2.Http2Error.INTERNAL_ERROR;
import static io.netty.handler.codec.http2.Http2Error.PROTOCOL_ERROR;
import static io.netty.handler.codec.http2.Http2Exception.connectionError;
import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;

/**
 * Default implementation of {@link Http2ConnectionEncoder}.
 */
@UnstableApi
public class DefaultHttp2ConnectionEncoder implements Http2ConnectionEncoder, Http2SettingsReceivedConsumer {
    private final Http2FrameWriter frameWriter;
    private final Http2Connection connection;
    private Http2LifecycleManager lifecycleManager;
    // We prefer ArrayDeque to LinkedList because later will produce more GC.
    // This initial capacity is plenty for SETTINGS traffic.
    private final Queue<Http2Settings> outstandingLocalSettingsQueue = new ArrayDeque<Http2Settings>(4);
    private Queue<Http2Settings> outstandingRemoteSettingsQueue;

    public DefaultHttp2ConnectionEncoder(final Http2Connection connection, final Http2FrameWriter frameWriter) {
        this.connection = checkNotNull(connection, "connection");
        this.frameWriter = checkNotNull(frameWriter, "frameWriter");
        if (connection.remote().flowController() == null) {
            connection.remote().flowController(new DefaultHttp2RemoteFlowController(connection));
        }
    }

    @Override
    public void lifecycleManager(final Http2LifecycleManager lifecycleManager) {
        this.lifecycleManager = checkNotNull(lifecycleManager, "lifecycleManager");
    }

    @Override
    public Http2FrameWriter frameWriter() {
        return frameWriter;
    }

    @Override
    public Http2Connection connection() {
        return connection;
    }

    @Override
    public final Http2RemoteFlowController flowController() {
        return connection().remote().flowController();
    }

    @Override
    public void remoteSettings(final Http2Settings settings) throws Http2Exception {
        final Boolean pushEnabled = settings.pushEnabled();
        final Http2FrameWriter.Configuration config = configuration();
        final Http2HeadersEncoder.Configuration outboundHeaderConfig = config.headersConfiguration();
        final Http2FrameSizePolicy outboundFrameSizePolicy = config.frameSizePolicy();
        if (pushEnabled != null) {
            if (!connection.isServer() && pushEnabled) {
                throw connectionError(PROTOCOL_ERROR,
                    "Client received a value of ENABLE_PUSH specified to other than 0");
            }
            connection.remote().allowPushTo(pushEnabled);
        }

        final Long maxConcurrentStreams = settings.maxConcurrentStreams();
        if (maxConcurrentStreams != null) {
            connection.local().maxActiveStreams((int) min(maxConcurrentStreams, MAX_VALUE));
        }

        final Long headerTableSize = settings.headerTableSize();
        if (headerTableSize != null) {
            outboundHeaderConfig.maxHeaderTableSize((int) min(headerTableSize, MAX_VALUE));
        }

        final Long maxHeaderListSize = settings.maxHeaderListSize();
        if (maxHeaderListSize != null) {
            outboundHeaderConfig.maxHeaderListSize(maxHeaderListSize);
        }

        final Integer maxFrameSize = settings.maxFrameSize();
        if (maxFrameSize != null) {
            outboundFrameSizePolicy.maxFrameSize(maxFrameSize);
        }

        final Integer initialWindowSize = settings.initialWindowSize();
        if (initialWindowSize != null) {
            flowController().initialWindowSize(initialWindowSize);
        }
    }

    @Override
    public ChannelFuture writeData(final ChannelHandlerContext ctx, final int streamId, final ByteBuf data, final int padding,
                                   final boolean endOfStream, final ChannelPromise promise) {
        final Http2Stream stream;
        try {
            stream = requireStream(streamId);

            // Verify that the stream is in the appropriate state for sending DATA frames.
            switch (stream.state()) {
                case OPEN:
                case HALF_CLOSED_REMOTE:
                    // Allowed sending DATA frames in these states.
                    break;
                default:
                    throw new IllegalStateException("Stream " + stream.id() + " in unexpected state " + stream.state());
            }
        } catch (final Throwable e) {
            data.release();
            return promise.setFailure(e);
        }

        // Hand control of the frame to the flow controller.
        flowController().addFlowControlled(stream,
                new FlowControlledData(stream, data, padding, endOfStream, promise));
        return promise;
    }

    @Override
    public ChannelFuture writeHeaders(final ChannelHandlerContext ctx, final int streamId, final Http2Headers headers, final int padding,
                                      final boolean endStream, final ChannelPromise promise) {
        return writeHeaders(ctx, streamId, headers, 0, DEFAULT_PRIORITY_WEIGHT, false, padding, endStream, promise);
    }

    private static boolean validateHeadersSentState(final Http2Stream stream, final Http2Headers headers, final boolean isServer,
                                                    final boolean endOfStream) {
        final boolean isInformational = isServer && HttpStatusClass.valueOf(headers.status()) == INFORMATIONAL;
        if ((isInformational || !endOfStream) && stream.isHeadersSent() || stream.isTrailersSent()) {
            throw new IllegalStateException("Stream " + stream.id() + " sent too many headers EOS: " + endOfStream);
        }
        return isInformational;
    }

    @Override
    public ChannelFuture writeHeaders(final ChannelHandlerContext ctx, final int streamId,
            final Http2Headers headers, final int streamDependency, final short weight,
            final boolean exclusive, final int padding, final boolean endOfStream, ChannelPromise promise) {
        try {
            Http2Stream stream = connection.stream(streamId);
            if (stream == null) {
                try {
                    // We don't create the stream in a `halfClosed` state because if this is an initial
                    // HEADERS frame we don't want the connection state to signify that the HEADERS have
                    // been sent until after they have been encoded and placed in the outbound buffer.
                    // Therefore, we let the `LifeCycleManager` will take care of transitioning the state
                    // as appropriate.
                    stream = connection.local().createStream(streamId, /*endOfStream*/ false);
                } catch (final Http2Exception cause) {
                    if (connection.remote().mayHaveCreatedStream(streamId)) {
                        promise.tryFailure(new IllegalStateException("Stream no longer exists: " + streamId, cause));
                        return promise;
                    }
                    throw cause;
                }
            } else {
                switch (stream.state()) {
                    case RESERVED_LOCAL:
                        stream.open(endOfStream);
                        break;
                    case OPEN:
                    case HALF_CLOSED_REMOTE:
                        // Allowed sending headers in these states.
                        break;
                    default:
                        throw new IllegalStateException("Stream " + stream.id() + " in unexpected state " +
                                                        stream.state());
                }
            }

            // Trailing headers must go through flow control if there are other frames queued in flow control
            // for this stream.
            final Http2RemoteFlowController flowController = flowController();
            if (!endOfStream || !flowController.hasFlowControlled(stream)) {
                // The behavior here should mirror that in FlowControlledHeaders

                promise = promise.unvoid();
                final boolean isInformational = validateHeadersSentState(stream, headers, connection.isServer(), endOfStream);

                final ChannelFuture future = frameWriter.writeHeaders(ctx, streamId, headers, streamDependency,
                                                                weight, exclusive, padding, endOfStream, promise);
                // Writing headers may fail during the encode state if they violate HPACK limits.
                final Throwable failureCause = future.cause();
                if (failureCause == null) {
                    // Synchronously set the headersSent flag to ensure that we do not subsequently write
                    // other headers containing pseudo-header fields.
                    //
                    // This just sets internal stream state which is used elsewhere in the codec and doesn't
                    // necessarily mean the write will complete successfully.
                    stream.headersSent(isInformational);

                    if (!future.isSuccess()) {
                        // Either the future is not done or failed in the meantime.
                        notifyLifecycleManagerOnError(future, ctx);
                    }
                } else {
                    lifecycleManager.onError(ctx, true, failureCause);
                }

                if (endOfStream) {
                    // Must handle calling onError before calling closeStreamLocal, otherwise the error handler will
                    // incorrectly think the stream no longer exists and so may not send RST_STREAM or perform similar
                    // appropriate action.
                    lifecycleManager.closeStreamLocal(stream, future);
                }

                return future;
            } else {
                // Pass headers to the flow-controller so it can maintain their sequence relative to DATA frames.
                flowController.addFlowControlled(stream,
                        new FlowControlledHeaders(stream, headers, streamDependency, weight, exclusive, padding,
                                                  true, promise));
                return promise;
            }
        } catch (final Throwable t) {
            lifecycleManager.onError(ctx, true, t);
            promise.tryFailure(t);
            return promise;
        }
    }

    @Override
    public ChannelFuture writePriority(final ChannelHandlerContext ctx, final int streamId, final int streamDependency, final short weight,
                                       final boolean exclusive, final ChannelPromise promise) {
        return frameWriter.writePriority(ctx, streamId, streamDependency, weight, exclusive, promise);
    }

    @Override
    public ChannelFuture writeRstStream(final ChannelHandlerContext ctx, final int streamId, final long errorCode,
                                        final ChannelPromise promise) {
        // Delegate to the lifecycle manager for proper updating of connection state.
        return lifecycleManager.resetStream(ctx, streamId, errorCode, promise);
    }

    @Override
    public ChannelFuture writeSettings(final ChannelHandlerContext ctx, final Http2Settings settings,
                                       final ChannelPromise promise) {
        outstandingLocalSettingsQueue.add(settings);
        try {
            final Boolean pushEnabled = settings.pushEnabled();
            if (pushEnabled != null && connection.isServer()) {
                throw connectionError(PROTOCOL_ERROR, "Server sending SETTINGS frame with ENABLE_PUSH specified");
            }
        } catch (final Throwable e) {
            return promise.setFailure(e);
        }

        return frameWriter.writeSettings(ctx, settings, promise);
    }

    @Override
    public ChannelFuture writeSettingsAck(final ChannelHandlerContext ctx, final ChannelPromise promise) {
        if (outstandingRemoteSettingsQueue == null) {
            return frameWriter.writeSettingsAck(ctx, promise);
        }
        final Http2Settings settings = outstandingRemoteSettingsQueue.poll();
        if (settings == null) {
            return promise.setFailure(new Http2Exception(INTERNAL_ERROR, "attempted to write a SETTINGS ACK with no " +
                    " pending SETTINGS"));
        }
        final SimpleChannelPromiseAggregator aggregator = new SimpleChannelPromiseAggregator(promise, ctx.channel(),
                ctx.executor());
        // Acknowledge receipt of the settings. We should do this before we process the settings to ensure our
        // remote peer applies these settings before any subsequent frames that we may send which depend upon
        // these new settings. See https://github.com/netty/netty/issues/6520.
        frameWriter.writeSettingsAck(ctx, aggregator.newPromise());

        // We create a "new promise" to make sure that status from both the write and the application are taken into
        // account independently.
        final ChannelPromise applySettingsPromise = aggregator.newPromise();
        try {
            remoteSettings(settings);
            applySettingsPromise.setSuccess();
        } catch (final Throwable e) {
            applySettingsPromise.setFailure(e);
            lifecycleManager.onError(ctx, true, e);
        }
        return aggregator.doneAllocatingPromises();
    }

    @Override
    public ChannelFuture writePing(final ChannelHandlerContext ctx, final boolean ack, final long data, final ChannelPromise promise) {
        return frameWriter.writePing(ctx, ack, data, promise);
    }

    @Override
    public ChannelFuture writePushPromise(final ChannelHandlerContext ctx, final int streamId, final int promisedStreamId,
                                          final Http2Headers headers, final int padding, ChannelPromise promise) {
        try {
            if (connection.goAwayReceived()) {
                throw connectionError(PROTOCOL_ERROR, "Sending PUSH_PROMISE after GO_AWAY received.");
            }

            final Http2Stream stream = requireStream(streamId);
            // Reserve the promised stream.
            connection.local().reservePushStream(promisedStreamId, stream);

            promise = promise.unvoid();
            final ChannelFuture future = frameWriter.writePushPromise(ctx, streamId, promisedStreamId, headers, padding,
                                                                promise);
            // Writing headers may fail during the encode state if they violate HPACK limits.
            final Throwable failureCause = future.cause();
            if (failureCause == null) {
                // This just sets internal stream state which is used elsewhere in the codec and doesn't
                // necessarily mean the write will complete successfully.
                stream.pushPromiseSent();

                if (!future.isSuccess()) {
                    // Either the future is not done or failed in the meantime.
                    notifyLifecycleManagerOnError(future, ctx);
                }
            } else {
                lifecycleManager.onError(ctx, true, failureCause);
            }
            return future;
        } catch (final Throwable t) {
            lifecycleManager.onError(ctx, true, t);
            promise.tryFailure(t);
            return promise;
        }
    }

    @Override
    public ChannelFuture writeGoAway(final ChannelHandlerContext ctx, final int lastStreamId, final long errorCode, final ByteBuf debugData,
                                     final ChannelPromise promise) {
        return lifecycleManager.goAway(ctx, lastStreamId, errorCode, debugData, promise);
    }

    @Override
    public ChannelFuture writeWindowUpdate(final ChannelHandlerContext ctx, final int streamId, final int windowSizeIncrement,
                                           final ChannelPromise promise) {
        return promise.setFailure(new UnsupportedOperationException("Use the Http2[Inbound|Outbound]FlowController" +
                " objects to control window sizes"));
    }

    @Override
    public ChannelFuture writeFrame(final ChannelHandlerContext ctx, final byte frameType, final int streamId, final Http2Flags flags,
                                    final ByteBuf payload, final ChannelPromise promise) {
        return frameWriter.writeFrame(ctx, frameType, streamId, flags, payload, promise);
    }

    @Override
    public void close() {
        frameWriter.close();
    }

    @Override
    public Http2Settings pollSentSettings() {
        return outstandingLocalSettingsQueue.poll();
    }

    @Override
    public Configuration configuration() {
        return frameWriter.configuration();
    }

    private Http2Stream requireStream(final int streamId) {
        final Http2Stream stream = connection.stream(streamId);
        if (stream == null) {
            final String message;
            if (connection.streamMayHaveExisted(streamId)) {
                message = "Stream no longer exists: " + streamId;
            } else {
                message = "Stream does not exist: " + streamId;
            }
            throw new IllegalArgumentException(message);
        }
        return stream;
    }

    @Override
    public void consumeReceivedSettings(final Http2Settings settings) {
        if (outstandingRemoteSettingsQueue == null) {
            outstandingRemoteSettingsQueue = new ArrayDeque<Http2Settings>(2);
        }
        outstandingRemoteSettingsQueue.add(settings);
    }

    /**
     * Wrap a DATA frame so it can be written subject to flow-control. Note that this implementation assumes it
     * only writes padding once for the entire payload as opposed to writing it once per-frame. This makes the
     * {@link #size} calculation deterministic thereby greatly simplifying the implementation.
     * <p>
     * If frame-splitting is required to fit within max-frame-size and flow-control constraints we ensure that
     * the passed promise is not completed until last frame write.
     * </p>
     */
    private final class FlowControlledData extends FlowControlledBase {
        private final CoalescingBufferQueue queue;
        private int dataSize;

        FlowControlledData(final Http2Stream stream, final ByteBuf buf, final int padding, final boolean endOfStream,
                           final ChannelPromise promise) {
            super(stream, padding, endOfStream, promise);
            queue = new CoalescingBufferQueue(promise.channel());
            queue.add(buf, promise);
            dataSize = queue.readableBytes();
        }

        @Override
        public int size() {
            return dataSize + padding;
        }

        @Override
        public void error(final ChannelHandlerContext ctx, final Throwable cause) {
            queue.releaseAndFailAll(cause);
            // Don't update dataSize because we need to ensure the size() method returns a consistent size even after
            // error so we don't invalidate flow control when returning bytes to flow control.
            //
            // That said we will set dataSize and padding to 0 in the write(...) method if we cleared the queue
            // because of an error.
            lifecycleManager.onError(ctx, true, cause);
        }

        @Override
        public void write(final ChannelHandlerContext ctx, final int allowedBytes) {
            final int queuedData = queue.readableBytes();
            if (!endOfStream) {
                if (queuedData == 0) {
                    if (queue.isEmpty()) {
                        // When the queue is empty it means we did clear it because of an error(...) call
                        // (as otherwise we will have at least 1 entry in there), which will happen either when called
                        // explicit or when the write itself fails. In this case just set dataSize and padding to 0
                        // which will signal back that the whole frame was consumed.
                        //
                        // See https://github.com/netty/netty/issues/8707.
                        padding = dataSize = 0;
                    } else {
                        // There's no need to write any data frames because there are only empty data frames in the
                        // queue and it is not end of stream yet. Just complete their promises by getting the buffer
                        // corresponding to 0 bytes and writing it to the channel (to preserve notification order).
                        final ChannelPromise writePromise = ctx.newPromise().addListener(this);
                        ctx.write(queue.remove(0, writePromise), writePromise);
                    }
                    return;
                }

                if (allowedBytes == 0) {
                    return;
                }
            }

            // Determine how much data to write.
            final int writableData = min(queuedData, allowedBytes);
            final ChannelPromise writePromise = ctx.newPromise().addListener(this);
            final ByteBuf toWrite = queue.remove(writableData, writePromise);
            dataSize = queue.readableBytes();

            // Determine how much padding to write.
            final int writablePadding = min(allowedBytes - writableData, padding);
            padding -= writablePadding;

            // Write the frame(s).
            frameWriter().writeData(ctx, stream.id(), toWrite, writablePadding,
                    endOfStream && size() == 0, writePromise);
        }

        @Override
        public boolean merge(final ChannelHandlerContext ctx, final Http2RemoteFlowController.FlowControlled next) {
            final FlowControlledData nextData;
            if (FlowControlledData.class != next.getClass() ||
                MAX_VALUE - (nextData = (FlowControlledData) next).size() < size()) {
                return false;
            }
            nextData.queue.copyTo(queue);
            dataSize = queue.readableBytes();
            // Given that we're merging data into a frame it doesn't really make sense to accumulate padding.
            padding = Math.max(padding, nextData.padding);
            endOfStream = nextData.endOfStream;
            return true;
        }
    }

    private void notifyLifecycleManagerOnError(final ChannelFuture future, final ChannelHandlerContext ctx) {
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                final Throwable cause = future.cause();
                if (cause != null) {
                    lifecycleManager.onError(ctx, true, cause);
                }
            }
        });
    }

    /**
     * Wrap headers so they can be written subject to flow-control. While headers do not have cost against the
     * flow-control window their order with respect to other frames must be maintained, hence if a DATA frame is
     * blocked on flow-control a HEADER frame must wait until this frame has been written.
     */
    private final class FlowControlledHeaders extends FlowControlledBase {
        private final Http2Headers headers;
        private final int streamDependency;
        private final short weight;
        private final boolean exclusive;

        FlowControlledHeaders(final Http2Stream stream, final Http2Headers headers, final int streamDependency, final short weight,
                              final boolean exclusive, final int padding, final boolean endOfStream, final ChannelPromise promise) {
            super(stream, padding, endOfStream, promise.unvoid());
            this.headers = headers;
            this.streamDependency = streamDependency;
            this.weight = weight;
            this.exclusive = exclusive;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void error(final ChannelHandlerContext ctx, final Throwable cause) {
            if (ctx != null) {
                lifecycleManager.onError(ctx, true, cause);
            }
            promise.tryFailure(cause);
        }

        @Override
        public void write(final ChannelHandlerContext ctx, final int allowedBytes) {
            final boolean isInformational = validateHeadersSentState(stream, headers, connection.isServer(), endOfStream);
            // The code is currently requiring adding this listener before writing, in order to call onError() before
            // closeStreamLocal().
            promise.addListener(this);

            final ChannelFuture f = frameWriter.writeHeaders(ctx, stream.id(), headers, streamDependency, weight, exclusive,
                                                       padding, endOfStream, promise);
            // Writing headers may fail during the encode state if they violate HPACK limits.
            final Throwable failureCause = f.cause();
            if (failureCause == null) {
                // This just sets internal stream state which is used elsewhere in the codec and doesn't
                // necessarily mean the write will complete successfully.
                stream.headersSent(isInformational);
            }
        }

        @Override
        public boolean merge(final ChannelHandlerContext ctx, final Http2RemoteFlowController.FlowControlled next) {
            return false;
        }
    }

    /**
     * Common base type for payloads to deliver via flow-control.
     */
    public abstract class FlowControlledBase implements Http2RemoteFlowController.FlowControlled,
            ChannelFutureListener {
        protected final Http2Stream stream;
        protected ChannelPromise promise;
        protected boolean endOfStream;
        protected int padding;

        FlowControlledBase(final Http2Stream stream, final int padding, final boolean endOfStream,
                           final ChannelPromise promise) {
            checkPositiveOrZero(padding, "padding");
            this.padding = padding;
            this.endOfStream = endOfStream;
            this.stream = stream;
            this.promise = promise;
        }

        @Override
        public void writeComplete() {
            if (endOfStream) {
                lifecycleManager.closeStreamLocal(stream, promise);
            }
        }

        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                error(flowController().channelHandlerContext(), future.cause());
            }
        }
    }
}
