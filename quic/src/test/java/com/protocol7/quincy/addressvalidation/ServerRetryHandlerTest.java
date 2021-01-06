package com.protocol7.quincy.addressvalidation;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.netty2.api.QuicTokenHandler;
import com.protocol7.quincy.netty2.impl.InsecureQuicTokenHandler;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServerRetryHandlerTest {

  @Mock PipelineContext ctx;
  private final QuicTokenHandler tokenHandler = InsecureQuicTokenHandler.INSTANCE;

  private final ServerRetryHandler handler =
      new ServerRetryHandler(of(InsecureQuicTokenHandler.INSTANCE));
  private final InetSocketAddress address = TestUtil.getTestAddress();

  @Before
  public void setUp() {
    when(ctx.getVersion()).thenReturn(Version.DRAFT_29);
    when(ctx.getPeerAddress()).thenReturn(TestUtil.getTestAddress());
    when(ctx.getState()).thenReturn(State.Started);
  }

  @Test
  public void retry() {
    final InitialPacket initialPacket = p(ConnectionId.random(), Optional.empty());
    handler.onReceivePacket(initialPacket, ctx);

    assertToken(initialPacket.getSourceConnectionId());

    // initial packet was not propagated
    verify(ctx, never()).next(any(Packet.class));
  }

  @Test
  public void withToken() {
    final ConnectionId destConnId = ConnectionId.random();
    final ByteBuf tokenBB = Unpooled.buffer();
    tokenHandler.writeToken(tokenBB, destConnId.asByteBuffer(), address);
    final Optional<byte[]> token = Optional.of(Bytes.drainToArray(tokenBB));

    final InitialPacket initialPacket = p(destConnId, token);
    handler.onReceivePacket(initialPacket, ctx);

    // no retry sent
    verify(ctx, never()).sendPacket(any(Packet.class));

    // initial packet propagated
    verify(ctx).next(initialPacket);
  }

  @Test
  public void withInvalidToken() {
    final InitialPacket initialPacket =
        p(ConnectionId.random(), of("this is not a token".getBytes()));
    handler.onReceivePacket(initialPacket, ctx);

    assertToken(initialPacket.getSourceConnectionId());

    // initial packet was not propagated
    verify(ctx, never()).next(any(Packet.class));
  }

  private void assertToken(final ConnectionId expectedDestConnId) {
    final ArgumentCaptor<RetryPacket> retryCaptor = ArgumentCaptor.forClass(RetryPacket.class);
    verify(ctx).sendPacket(retryCaptor.capture());

    final RetryPacket retry = retryCaptor.getValue();
    assertEquals(expectedDestConnId, retry.getDestinationConnectionId());
    assertNotEquals(
        -1, tokenHandler.validateToken(Unpooled.wrappedBuffer(retry.getRetryToken()), address));
  }

  @Test
  public void verifyState() {
    // retry handler should only validate messages in the Started state.
    // TODO if this correct?
    when(ctx.getState()).thenReturn(State.BeforeHello);

    final InitialPacket initialPacket = p(ConnectionId.random(), empty());
    handler.onReceivePacket(initialPacket, ctx);

    // no retry packet sent
    verify(ctx, never()).sendPacket(any(RetryPacket.class));

    // initial instead packet propagated
    verify(ctx).next(initialPacket);
  }

  private InitialPacket p(final ConnectionId destConnId, final Optional<byte[]> token) {

    return InitialPacket.create(
        destConnId,
        ConnectionId.random(),
        PacketNumber.MIN,
        Version.DRAFT_29,
        token,
        new PaddingFrame(1));
  }
}
