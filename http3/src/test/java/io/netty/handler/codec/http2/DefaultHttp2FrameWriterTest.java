/*
 * Copyright 2017 The Netty Project
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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Tests for {@link DefaultHttp2FrameWriter}. */
public class DefaultHttp2FrameWriterTest {
  private DefaultHttp2FrameWriter frameWriter;

  private ByteBuf outbound;

  private ByteBuf expectedOutbound;

  private ChannelPromise promise;

  private Http2HeadersEncoder http2HeadersEncoder;

  @Mock private Channel channel;

  @Mock private ChannelFuture future;

  @Mock private ChannelHandlerContext ctx;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    frameWriter = new DefaultHttp2FrameWriter();

    outbound = Unpooled.buffer();

    expectedOutbound = Unpooled.EMPTY_BUFFER;

    promise = new DefaultChannelPromise(channel, ImmediateEventExecutor.INSTANCE);

    http2HeadersEncoder = new DefaultHttp2HeadersEncoder();

    final Answer<Object> answer =
        new Answer<Object>() {
          @Override
          public Object answer(final InvocationOnMock var1) throws Throwable {
            final Object msg = var1.getArgument(0);
            if (msg instanceof ByteBuf) {
              outbound.writeBytes((ByteBuf) msg);
            }
            ReferenceCountUtil.release(msg);
            return future;
          }
        };
    when(ctx.write(any())).then(answer);
    when(ctx.write(any(), any(ChannelPromise.class))).then(answer);
    when(ctx.alloc()).thenReturn(UnpooledByteBufAllocator.DEFAULT);
    when(ctx.channel()).thenReturn(channel);
    when(ctx.executor()).thenReturn(ImmediateEventExecutor.INSTANCE);
  }

  @After
  public void tearDown() throws Exception {
    outbound.release();
    expectedOutbound.release();
    frameWriter.close();
  }

  @Test
  public void writeHeaders() throws Exception {
    final int streamId = 1;
    final Http2Headers headers =
        new DefaultHttp2Headers().method("GET").path("/").authority("foo.com").scheme("https");

    frameWriter.writeHeaders(ctx, streamId, headers, 0, true, promise);

    final byte[] expectedPayload = headerPayload(streamId, headers);
    final byte[] expectedFrameBytes = {
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x0a, // payload length = 10
      (byte) 0x01, // payload type = 1
      (byte) 0x05, // flags = (0x01 | 0x04)
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x01 // stream id = 1
    };
    expectedOutbound = Unpooled.copiedBuffer(expectedFrameBytes, expectedPayload);
    assertEquals(expectedOutbound, outbound);
  }

  @Test
  public void writeHeadersWithPadding() throws Exception {
    final int streamId = 1;
    final Http2Headers headers =
        new DefaultHttp2Headers().method("GET").path("/").authority("foo.com").scheme("https");

    frameWriter.writeHeaders(ctx, streamId, headers, 5, true, promise);

    final byte[] expectedPayload = headerPayload(streamId, headers, (byte) 4);
    final byte[] expectedFrameBytes = {
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x0f, // payload length = 16
      (byte) 0x01, // payload type = 1
      (byte) 0x0d, // flags = (0x01 | 0x04 | 0x08)
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x01 // stream id = 1
    };
    expectedOutbound = Unpooled.copiedBuffer(expectedFrameBytes, expectedPayload);
    assertEquals(expectedOutbound, outbound);
  }

  @Test
  public void writeHeadersNotEndStream() throws Exception {
    final int streamId = 1;
    final Http2Headers headers =
        new DefaultHttp2Headers().method("GET").path("/").authority("foo.com").scheme("https");

    frameWriter.writeHeaders(ctx, streamId, headers, 0, false, promise);

    final byte[] expectedPayload = headerPayload(streamId, headers);
    final byte[] expectedFrameBytes = {
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x0a, // payload length = 10
      (byte) 0x01, // payload type = 1
      (byte) 0x04, // flags = 0x04
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x01 // stream id = 1
    };
    final ByteBuf expectedOutbound = Unpooled.copiedBuffer(expectedFrameBytes, expectedPayload);
    assertEquals(expectedOutbound, outbound);
  }

  /**
   * Test large headers that exceed {@link DefaultHttp2FrameWriter#maxFrameSize} the remaining
   * headers will be sent in a CONTINUATION frame
   */
  @Test
  public void writeLargeHeaders() throws Exception {
    final int streamId = 1;
    Http2Headers headers =
        new DefaultHttp2Headers().method("GET").path("/").authority("foo.com").scheme("https");
    headers = dummyHeaders(headers, 20);

    http2HeadersEncoder.configuration().maxHeaderListSize(Integer.MAX_VALUE);
    frameWriter.headersConfiguration().maxHeaderListSize(Integer.MAX_VALUE);
    frameWriter.maxFrameSize(Http2CodecUtil.MAX_FRAME_SIZE_LOWER_BOUND);
    frameWriter.writeHeaders(ctx, streamId, headers, 0, true, promise);

    final byte[] expectedPayload = headerPayload(streamId, headers);

    // First frame: HEADER(length=0x4000, flags=0x01)
    assertEquals(Http2CodecUtil.MAX_FRAME_SIZE_LOWER_BOUND, outbound.readUnsignedMedium());
    assertEquals(0x01, outbound.readByte());
    assertEquals(0x01, outbound.readByte());
    assertEquals(streamId, outbound.readInt());

    final byte[] firstPayload = new byte[Http2CodecUtil.MAX_FRAME_SIZE_LOWER_BOUND];
    outbound.readBytes(firstPayload);

    final int remainPayloadLength =
        expectedPayload.length - Http2CodecUtil.MAX_FRAME_SIZE_LOWER_BOUND;
    // Second frame: CONTINUATION(length=remainPayloadLength, flags=0x04)
    assertEquals(remainPayloadLength, outbound.readUnsignedMedium());
    assertEquals(0x09, outbound.readByte());
    assertEquals(0x04, outbound.readByte());
    assertEquals(streamId, outbound.readInt());

    final byte[] secondPayload = new byte[remainPayloadLength];
    outbound.readBytes(secondPayload);

    assertArrayEquals(Arrays.copyOfRange(expectedPayload, 0, firstPayload.length), firstPayload);
    assertArrayEquals(
        Arrays.copyOfRange(expectedPayload, firstPayload.length, expectedPayload.length),
        secondPayload);
  }

  @Test
  public void writeFrameZeroPayload() throws Exception {
    frameWriter.writeFrame(ctx, (byte) 0xf, 0, new Http2Flags(), Unpooled.EMPTY_BUFFER, promise);

    final byte[] expectedFrameBytes = {
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00, // payload length
      (byte) 0x0f, // payload type
      (byte) 0x00, // flags
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00 // stream id
    };

    expectedOutbound = Unpooled.wrappedBuffer(expectedFrameBytes);
    assertEquals(expectedOutbound, outbound);
  }

  @Test
  public void writeFrameHasPayload() throws Exception {
    final byte[] payload = {(byte) 0x01, (byte) 0x03, (byte) 0x05, (byte) 0x07, (byte) 0x09};

    // will auto release after frameWriter.writeFrame succeed
    final ByteBuf payloadByteBuf = Unpooled.wrappedBuffer(payload);
    frameWriter.writeFrame(ctx, (byte) 0xf, 0, new Http2Flags(), payloadByteBuf, promise);

    final byte[] expectedFrameHeaderBytes = {
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x05, // payload length
      (byte) 0x0f, // payload type
      (byte) 0x00, // flags
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00,
      (byte) 0x00 // stream id
    };
    expectedOutbound = Unpooled.copiedBuffer(expectedFrameHeaderBytes, payload);
    assertEquals(expectedOutbound, outbound);
  }

  private byte[] headerPayload(final int streamId, final Http2Headers headers, final byte padding)
      throws Http2Exception, IOException {
    if (padding == 0) {
      return headerPayload(streamId, headers);
    }

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      outputStream.write(padding);
      outputStream.write(headerPayload(streamId, headers));
      outputStream.write(new byte[padding]);
      return outputStream.toByteArray();
    } finally {
      outputStream.close();
    }
  }

  private byte[] headerPayload(final int streamId, final Http2Headers headers)
      throws Http2Exception {
    final ByteBuf byteBuf = Unpooled.buffer();
    try {
      http2HeadersEncoder.encodeHeaders(streamId, headers, byteBuf);
      final byte[] bytes = new byte[byteBuf.readableBytes()];
      byteBuf.readBytes(bytes);
      return bytes;
    } finally {
      byteBuf.release();
    }
  }

  private static Http2Headers dummyHeaders(final Http2Headers headers, final int times) {
    final String largeValue = repeat("dummy-value", 100);
    for (int i = 0; i < times; i++) {
      headers.add(String.format("dummy-%d", i), largeValue);
    }
    return headers;
  }

  private static String repeat(final String str, final int count) {
    return String.format(String.format("%%%ds", count), " ").replace(" ", str);
  }
}
