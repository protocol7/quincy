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

import static com.protocol7.quincy.http3.Http2CodecUtil.MAX_HEADER_LIST_SIZE;
import static com.protocol7.quincy.http3.Http2CodecUtil.MAX_HEADER_TABLE_SIZE;
import static io.netty.util.ReferenceCountUtil.release;
import static java.lang.Math.min;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import junit.framework.AssertionFailedError;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Utilities for the integration tests. */
public final class Http2TestUtil {
  /** Interface that allows for running a operation that throws a {@link Http2Exception}. */
  interface Http2Runnable {
    void run() throws Http2Exception;
  }

  /** Runs the given operation within the event loop thread of the given {@link Channel}. */
  static void runInChannel(final Channel channel, final Http2Runnable runnable) {
    channel
        .eventLoop()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                try {
                  runnable.run();
                } catch (final Http2Exception e) {
                  throw new RuntimeException(e);
                }
              }
            });
  }

  /** Returns a byte array filled with random data. */
  public static byte[] randomBytes() {
    return randomBytes(100);
  }

  /** Returns a byte array filled with random data. */
  public static byte[] randomBytes(final int size) {
    final byte[] data = new byte[size];
    new Random().nextBytes(data);
    return data;
  }

  /** Returns an {@link AsciiString} that wraps a randomly-filled byte array. */
  public static AsciiString randomString() {
    return new AsciiString(randomBytes());
  }

  public static CharSequence of(final String s) {
    return s;
  }

  public static HpackEncoder newTestEncoder() {
    try {
      return newTestEncoder(true, MAX_HEADER_LIST_SIZE, MAX_HEADER_TABLE_SIZE);
    } catch (final Http2Exception e) {
      throw new Error("max size not allowed?", e);
    }
  }

  public static HpackEncoder newTestEncoder(
      final boolean ignoreMaxHeaderListSize,
      final long maxHeaderListSize,
      final long maxHeaderTableSize)
      throws Http2Exception {
    final HpackEncoder hpackEncoder = new HpackEncoder();
    final ByteBuf buf = Unpooled.buffer();
    try {
      hpackEncoder.setMaxHeaderTableSize(buf, maxHeaderTableSize);
      hpackEncoder.setMaxHeaderListSize(maxHeaderListSize);
    } finally {
      buf.release();
    }
    return hpackEncoder;
  }

  public static HpackDecoder newTestDecoder() {
    try {
      return newTestDecoder(MAX_HEADER_LIST_SIZE, MAX_HEADER_TABLE_SIZE);
    } catch (final Http2Exception e) {
      throw new Error("max size not allowed?", e);
    }
  }

  public static HpackDecoder newTestDecoder(
      final long maxHeaderListSize, final long maxHeaderTableSize) throws Http2Exception {
    final HpackDecoder hpackDecoder = new HpackDecoder(maxHeaderListSize, 32);
    hpackDecoder.setMaxHeaderTableSize(maxHeaderTableSize);
    return hpackDecoder;
  }

  private Http2TestUtil() {}

  static class FrameAdapter extends ByteToMessageDecoder {
    private final Http2Connection connection;
    private final Http2FrameListener listener;
    private final DefaultHttp2FrameReader reader;
    private final CountDownLatch latch;

    FrameAdapter(final Http2FrameListener listener, final CountDownLatch latch) {
      this(null, listener, latch);
    }

    FrameAdapter(
        final Http2Connection connection,
        final Http2FrameListener listener,
        final CountDownLatch latch) {
      this(connection, new DefaultHttp2FrameReader(false), listener, latch);
    }

    FrameAdapter(
        final Http2Connection connection,
        final DefaultHttp2FrameReader reader,
        final Http2FrameListener listener,
        final CountDownLatch latch) {
      this.connection = connection;
      this.listener = listener;
      this.reader = reader;
      this.latch = latch;
    }

    private Http2Stream getOrCreateStream(final int streamId, final boolean halfClosed)
        throws Http2Exception {
      return getOrCreateStream(connection, streamId, halfClosed);
    }

    public static Http2Stream getOrCreateStream(
        final Http2Connection connection, final int streamId, final boolean halfClosed)
        throws Http2Exception {
      if (connection != null) {
        Http2Stream stream = connection.stream(streamId);
        if (stream == null) {
          if (connection.isServer() && streamId % 2 == 0
              || !connection.isServer() && streamId % 2 != 0) {
            stream = connection.local().createStream(streamId, halfClosed);
          } else {
            stream = connection.remote().createStream(streamId, halfClosed);
          }
        }
        return stream;
      }
      return null;
    }

    private void closeStream(final Http2Stream stream) {
      closeStream(stream, false);
    }

    protected void closeStream(final Http2Stream stream, final boolean dataRead) {
      if (stream != null) {
        stream.close();
      }
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out)
        throws Exception {
      reader.readFrame(
          ctx,
          in,
          new Http2FrameListener() {
            @Override
            public int onDataRead(
                final ChannelHandlerContext ctx,
                final int streamId,
                final ByteBuf data,
                final int padding,
                final boolean endOfStream)
                throws Http2Exception {
              final Http2Stream stream = getOrCreateStream(streamId, endOfStream);
              final int processed = listener.onDataRead(ctx, streamId, data, padding, endOfStream);
              if (endOfStream) {
                closeStream(stream, true);
              }
              latch.countDown();
              return processed;
            }

            @Override
            public void onHeadersRead(
                final ChannelHandlerContext ctx,
                final int streamId,
                final Http2Headers headers,
                final int padding,
                final boolean endStream)
                throws Http2Exception {
              final Http2Stream stream = getOrCreateStream(streamId, endStream);
              listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
              if (endStream) {
                closeStream(stream);
              }
              latch.countDown();
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
              final Http2Stream stream = getOrCreateStream(streamId, endStream);
              listener.onHeadersRead(
                  ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
              if (endStream) {
                closeStream(stream);
              }
              latch.countDown();
            }

            @Override
            public void onPriorityRead(
                final ChannelHandlerContext ctx,
                final int streamId,
                final int streamDependency,
                final short weight,
                final boolean exclusive)
                throws Http2Exception {
              listener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
              latch.countDown();
            }

            @Override
            public void onRstStreamRead(
                final ChannelHandlerContext ctx, final int streamId, final long errorCode)
                throws Http2Exception {
              final Http2Stream stream = getOrCreateStream(streamId, false);
              listener.onRstStreamRead(ctx, streamId, errorCode);
              closeStream(stream);
              latch.countDown();
            }

            @Override
            public void onSettingsAckRead(final ChannelHandlerContext ctx) throws Http2Exception {
              listener.onSettingsAckRead(ctx);
              latch.countDown();
            }

            @Override
            public void onSettingsRead(
                final ChannelHandlerContext ctx, final Http2Settings settings)
                throws Http2Exception {
              listener.onSettingsRead(ctx, settings);
              latch.countDown();
            }

            @Override
            public void onPingRead(final ChannelHandlerContext ctx, final long data)
                throws Http2Exception {
              listener.onPingRead(ctx, data);
              latch.countDown();
            }

            @Override
            public void onPingAckRead(final ChannelHandlerContext ctx, final long data)
                throws Http2Exception {
              listener.onPingAckRead(ctx, data);
              latch.countDown();
            }

            @Override
            public void onPushPromiseRead(
                final ChannelHandlerContext ctx,
                final int streamId,
                final int promisedStreamId,
                final Http2Headers headers,
                final int padding)
                throws Http2Exception {
              getOrCreateStream(promisedStreamId, false);
              listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
              latch.countDown();
            }

            @Override
            public void onGoAwayRead(
                final ChannelHandlerContext ctx,
                final int lastStreamId,
                final long errorCode,
                final ByteBuf debugData)
                throws Http2Exception {
              listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
              latch.countDown();
            }

            @Override
            public void onWindowUpdateRead(
                final ChannelHandlerContext ctx, final int streamId, final int windowSizeIncrement)
                throws Http2Exception {
              getOrCreateStream(streamId, false);
              listener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
              latch.countDown();
            }

            @Override
            public void onUnknownFrame(
                final ChannelHandlerContext ctx,
                final byte frameType,
                final int streamId,
                final Http2Flags flags,
                final ByteBuf payload)
                throws Http2Exception {
              listener.onUnknownFrame(ctx, frameType, streamId, flags, payload);
              latch.countDown();
            }
          });
    }
  }

  /**
   * A decorator around a {@link Http2FrameListener} that counts down the latch so that we can await
   * the completion of the request.
   */
  static class FrameCountDown implements Http2FrameListener {
    private final Http2FrameListener listener;
    private final CountDownLatch messageLatch;
    private final CountDownLatch settingsAckLatch;
    private final CountDownLatch dataLatch;
    private final CountDownLatch trailersLatch;
    private final CountDownLatch goAwayLatch;

    FrameCountDown(
        final Http2FrameListener listener,
        final CountDownLatch settingsAckLatch,
        final CountDownLatch messageLatch) {
      this(listener, settingsAckLatch, messageLatch, null, null);
    }

    FrameCountDown(
        final Http2FrameListener listener,
        final CountDownLatch settingsAckLatch,
        final CountDownLatch messageLatch,
        final CountDownLatch dataLatch,
        final CountDownLatch trailersLatch) {
      this(listener, settingsAckLatch, messageLatch, dataLatch, trailersLatch, messageLatch);
    }

    FrameCountDown(
        final Http2FrameListener listener,
        final CountDownLatch settingsAckLatch,
        final CountDownLatch messageLatch,
        final CountDownLatch dataLatch,
        final CountDownLatch trailersLatch,
        final CountDownLatch goAwayLatch) {
      this.listener = listener;
      this.messageLatch = messageLatch;
      this.settingsAckLatch = settingsAckLatch;
      this.dataLatch = dataLatch;
      this.trailersLatch = trailersLatch;
      this.goAwayLatch = goAwayLatch;
    }

    @Override
    public int onDataRead(
        final ChannelHandlerContext ctx,
        final int streamId,
        final ByteBuf data,
        final int padding,
        final boolean endOfStream)
        throws Http2Exception {
      final int numBytes = data.readableBytes();
      final int processed = listener.onDataRead(ctx, streamId, data, padding, endOfStream);
      messageLatch.countDown();
      if (dataLatch != null) {
        for (int i = 0; i < numBytes; ++i) {
          dataLatch.countDown();
        }
      }
      return processed;
    }

    @Override
    public void onHeadersRead(
        final ChannelHandlerContext ctx,
        final int streamId,
        final Http2Headers headers,
        final int padding,
        final boolean endStream)
        throws Http2Exception {
      listener.onHeadersRead(ctx, streamId, headers, padding, endStream);
      messageLatch.countDown();
      if (trailersLatch != null && endStream) {
        trailersLatch.countDown();
      }
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
      listener.onHeadersRead(
          ctx, streamId, headers, streamDependency, weight, exclusive, padding, endStream);
      messageLatch.countDown();
      if (trailersLatch != null && endStream) {
        trailersLatch.countDown();
      }
    }

    @Override
    public void onPriorityRead(
        final ChannelHandlerContext ctx,
        final int streamId,
        final int streamDependency,
        final short weight,
        final boolean exclusive)
        throws Http2Exception {
      listener.onPriorityRead(ctx, streamId, streamDependency, weight, exclusive);
      messageLatch.countDown();
    }

    @Override
    public void onRstStreamRead(
        final ChannelHandlerContext ctx, final int streamId, final long errorCode)
        throws Http2Exception {
      listener.onRstStreamRead(ctx, streamId, errorCode);
      messageLatch.countDown();
    }

    @Override
    public void onSettingsAckRead(final ChannelHandlerContext ctx) throws Http2Exception {
      listener.onSettingsAckRead(ctx);
      settingsAckLatch.countDown();
    }

    @Override
    public void onSettingsRead(final ChannelHandlerContext ctx, final Http2Settings settings)
        throws Http2Exception {
      listener.onSettingsRead(ctx, settings);
      messageLatch.countDown();
    }

    @Override
    public void onPingRead(final ChannelHandlerContext ctx, final long data) throws Http2Exception {
      listener.onPingRead(ctx, data);
      messageLatch.countDown();
    }

    @Override
    public void onPingAckRead(final ChannelHandlerContext ctx, final long data)
        throws Http2Exception {
      listener.onPingAckRead(ctx, data);
      messageLatch.countDown();
    }

    @Override
    public void onPushPromiseRead(
        final ChannelHandlerContext ctx,
        final int streamId,
        final int promisedStreamId,
        final Http2Headers headers,
        final int padding)
        throws Http2Exception {
      listener.onPushPromiseRead(ctx, streamId, promisedStreamId, headers, padding);
      messageLatch.countDown();
    }

    @Override
    public void onGoAwayRead(
        final ChannelHandlerContext ctx,
        final int lastStreamId,
        final long errorCode,
        final ByteBuf debugData)
        throws Http2Exception {
      listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
      goAwayLatch.countDown();
    }

    @Override
    public void onWindowUpdateRead(
        final ChannelHandlerContext ctx, final int streamId, final int windowSizeIncrement)
        throws Http2Exception {
      listener.onWindowUpdateRead(ctx, streamId, windowSizeIncrement);
      messageLatch.countDown();
    }

    @Override
    public void onUnknownFrame(
        final ChannelHandlerContext ctx,
        final byte frameType,
        final int streamId,
        final Http2Flags flags,
        final ByteBuf payload)
        throws Http2Exception {
      listener.onUnknownFrame(ctx, frameType, streamId, flags, payload);
      messageLatch.countDown();
    }
  }

  static ChannelPromise newVoidPromise(final Channel channel) {
    return new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE) {
      @Override
      public ChannelPromise addListener(
          final GenericFutureListener<? extends Future<? super Void>> listener) {
        throw new AssertionFailedError();
      }

      @Override
      public ChannelPromise addListeners(
          final GenericFutureListener<? extends Future<? super Void>>... listeners) {
        throw new AssertionFailedError();
      }

      @Override
      public boolean isVoid() {
        return true;
      }

      @Override
      public boolean tryFailure(final Throwable cause) {
        channel().pipeline().fireExceptionCaught(cause);
        return true;
      }

      @Override
      public ChannelPromise setFailure(final Throwable cause) {
        tryFailure(cause);
        return this;
      }

      @Override
      public ChannelPromise unvoid() {
        final ChannelPromise promise =
            new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);
        promise.addListener(
            new ChannelFutureListener() {
              @Override
              public void operationComplete(final ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                  channel().pipeline().fireExceptionCaught(future.cause());
                }
              }
            });
        return promise;
      }
    };
  }

  static final class TestStreamByteDistributorStreamState
      implements StreamByteDistributor.StreamState {
    private final Http2Stream stream;
    boolean isWriteAllowed;
    long pendingBytes;
    boolean hasFrame;

    TestStreamByteDistributorStreamState(
        final Http2Stream stream,
        final long pendingBytes,
        final boolean hasFrame,
        final boolean isWriteAllowed) {
      this.stream = stream;
      this.isWriteAllowed = isWriteAllowed;
      this.pendingBytes = pendingBytes;
      this.hasFrame = hasFrame;
    }

    @Override
    public Http2Stream stream() {
      return stream;
    }

    @Override
    public long pendingBytes() {
      return pendingBytes;
    }

    @Override
    public boolean hasFrame() {
      return hasFrame;
    }

    @Override
    public int windowSize() {
      return isWriteAllowed ? (int) min(pendingBytes, Integer.MAX_VALUE) : -1;
    }
  }

  static Http2FrameWriter mockedFrameWriter() {
    final Http2FrameWriter.Configuration configuration =
        new Http2FrameWriter.Configuration() {
          private final Http2HeadersEncoder.Configuration headerConfiguration =
              new Http2HeadersEncoder.Configuration() {
                @Override
                public void maxHeaderTableSize(final long max) {
                  // NOOP
                }

                @Override
                public long maxHeaderTableSize() {
                  return 0;
                }

                @Override
                public void maxHeaderListSize(final long max) {
                  // NOOP
                }

                @Override
                public long maxHeaderListSize() {
                  return 0;
                }
              };

          private final Http2FrameSizePolicy policy =
              new Http2FrameSizePolicy() {
                @Override
                public void maxFrameSize(final int max) {
                  // NOOP
                }

                @Override
                public int maxFrameSize() {
                  return 0;
                }
              };

          @Override
          public Http2HeadersEncoder.Configuration headersConfiguration() {
            return headerConfiguration;
          }

          @Override
          public Http2FrameSizePolicy frameSizePolicy() {
            return policy;
          }
        };

    final ConcurrentLinkedQueue<ByteBuf> buffers = new ConcurrentLinkedQueue<ByteBuf>();

    final Http2FrameWriter frameWriter = Mockito.mock(Http2FrameWriter.class);
    doAnswer(
            new Answer() {
              @Override
              public Object answer(final InvocationOnMock invocationOnMock) {
                for (; ; ) {
                  final ByteBuf buf = buffers.poll();
                  if (buf == null) {
                    break;
                  }
                  buf.release();
                }
                return null;
              }
            })
        .when(frameWriter)
        .close();

    when(frameWriter.configuration()).thenReturn(configuration);
    when(frameWriter.writeSettings(
            any(ChannelHandlerContext.class), any(Http2Settings.class), any(ChannelPromise.class)))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                return ((ChannelPromise) invocationOnMock.getArgument(2)).setSuccess();
              }
            });

    when(frameWriter.writeSettingsAck(any(ChannelHandlerContext.class), any(ChannelPromise.class)))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                return ((ChannelPromise) invocationOnMock.getArgument(1)).setSuccess();
              }
            });

    when(frameWriter.writeGoAway(
            any(ChannelHandlerContext.class),
            anyInt(),
            anyLong(),
            any(ByteBuf.class),
            any(ChannelPromise.class)))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                buffers.offer((ByteBuf) invocationOnMock.getArgument(3));
                return ((ChannelPromise) invocationOnMock.getArgument(4)).setSuccess();
              }
            });
    when(frameWriter.writeHeaders(
            any(ChannelHandlerContext.class),
            anyInt(),
            any(Http2Headers.class),
            anyInt(),
            anyBoolean(),
            any(ChannelPromise.class)))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                return ((ChannelPromise) invocationOnMock.getArgument(5)).setSuccess();
              }
            });

    when(frameWriter.writeHeaders(
            any(ChannelHandlerContext.class),
            anyInt(),
            any(Http2Headers.class),
            anyInt(),
            anyShort(),
            anyBoolean(),
            anyInt(),
            anyBoolean(),
            any(ChannelPromise.class)))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                return ((ChannelPromise) invocationOnMock.getArgument(8)).setSuccess();
              }
            });

    when(frameWriter.writeData(
            any(ChannelHandlerContext.class),
            anyInt(),
            any(ByteBuf.class),
            anyInt(),
            anyBoolean(),
            any(ChannelPromise.class)))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                buffers.offer((ByteBuf) invocationOnMock.getArgument(2));
                return ((ChannelPromise) invocationOnMock.getArgument(5)).setSuccess();
              }
            });

    when(frameWriter.writeRstStream(
            any(ChannelHandlerContext.class), anyInt(), anyLong(), any(ChannelPromise.class)))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                return ((ChannelPromise) invocationOnMock.getArgument(3)).setSuccess();
              }
            });

    when(frameWriter.writeWindowUpdate(
            any(ChannelHandlerContext.class), anyInt(), anyInt(), any(ChannelPromise.class)))
        .then(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                return ((ChannelPromise) invocationOnMock.getArgument(3)).setSuccess();
              }
            });

    when(frameWriter.writePushPromise(
            any(ChannelHandlerContext.class),
            anyInt(),
            anyInt(),
            any(Http2Headers.class),
            anyInt(),
            anyChannelPromise()))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                return ((ChannelPromise) invocationOnMock.getArgument(5)).setSuccess();
              }
            });

    when(frameWriter.writeFrame(
            any(ChannelHandlerContext.class),
            anyByte(),
            anyInt(),
            any(Http2Flags.class),
            any(ByteBuf.class),
            anyChannelPromise()))
        .thenAnswer(
            new Answer<ChannelFuture>() {
              @Override
              public ChannelFuture answer(final InvocationOnMock invocationOnMock) {
                buffers.offer((ByteBuf) invocationOnMock.getArgument(4));
                return ((ChannelPromise) invocationOnMock.getArgument(5)).setSuccess();
              }
            });
    return frameWriter;
  }

  static ChannelPromise anyChannelPromise() {
    return any(ChannelPromise.class);
  }

  static Http2Settings anyHttp2Settings() {
    return any(Http2Settings.class);
  }

  static ByteBuf bb(final String s) {
    return ByteBufUtil.writeUtf8(UnpooledByteBufAllocator.DEFAULT, s);
  }

  static void assertEqualsAndRelease(final Http2Frame expected, final Http2Frame actual) {
    try {
      assertEquals(expected, actual);
    } finally {
      release(expected);
      release(actual);
      // Will return -1 when not implements ReferenceCounted.
      assertTrue(ReferenceCountUtil.refCnt(expected) <= 0);
      assertTrue(ReferenceCountUtil.refCnt(actual) <= 0);
    }
  }
}
