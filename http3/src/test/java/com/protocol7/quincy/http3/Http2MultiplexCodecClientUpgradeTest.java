/*
 * Copyright 2018 The Netty Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

public class Http2MultiplexCodecClientUpgradeTest {

  @ChannelHandler.Sharable
  private final class NoopHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
      ctx.channel().close();
    }
  }

  private final class UpgradeHandler extends ChannelInboundHandlerAdapter {
    Http2Stream.State stateOnActive;
    int streamId;

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
      final Http2StreamChannel ch = (Http2StreamChannel) ctx.channel();
      stateOnActive = ch.stream().state();
      streamId = ch.stream().id();
      super.channelActive(ctx);
    }
  }

  private Http2MultiplexCodec newCodec(final ChannelHandler upgradeHandler) {
    final Http2MultiplexCodecBuilder builder =
        Http2MultiplexCodecBuilder.forClient(new NoopHandler());
    builder.withUpgradeStreamHandler(upgradeHandler);
    return builder.build();
  }

  @Test
  public void upgradeHandlerGetsActivated() throws Exception {
    final UpgradeHandler upgradeHandler = new UpgradeHandler();
    final Http2MultiplexCodec codec = newCodec(upgradeHandler);
    final EmbeddedChannel ch = new EmbeddedChannel(codec);

    codec.onHttpClientUpgrade();

    assertFalse(upgradeHandler.stateOnActive.localSideOpen());
    assertTrue(upgradeHandler.stateOnActive.remoteSideOpen());
    assertEquals(1, upgradeHandler.streamId);
    assertTrue(ch.finishAndReleaseAll());
  }

  @Test(expected = Http2Exception.class)
  public void clientUpgradeWithoutUpgradeHandlerThrowsHttp2Exception() throws Http2Exception {
    final Http2MultiplexCodec codec =
        Http2MultiplexCodecBuilder.forClient(new NoopHandler()).build();
    final EmbeddedChannel ch = new EmbeddedChannel(codec);
    try {
      codec.onHttpClientUpgrade();
    } finally {
      assertTrue(ch.finishAndReleaseAll());
    }
  }
}
