package com.protocol7.quincy.addressvalidation;

import static java.lang.System.currentTimeMillis;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.TestUtil;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.tls.KeyUtil;
import java.net.InetAddress;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ServerRetryHandlerTest {

  @Mock PipelineContext ctx;
  private final RetryToken retryToken =
      new RetryToken(KeyUtil.getPrivateKey("src/test/resources/server.der"));
  private final ServerRetryHandler handler =
      new ServerRetryHandler(retryToken, 10000, TimeUnit.MILLISECONDS);
  private final InetAddress address = TestUtil.getTestAddress().getAddress();

  @Before
  public void setUp() {
    when(ctx.getVersion()).thenReturn(Version.DRAFT_29);
    when(ctx.getPeerAddress()).thenReturn(TestUtil.getTestAddress());
    when(ctx.getState()).thenReturn(State.Started);
  }

  @Test
  public void retry() {
    final InitialPacket initialPacket = p(Optional.empty());
    handler.onReceivePacket(initialPacket, ctx);

    assertToken(initialPacket.getSourceConnectionId());

    // initial packet was not propagated
    verify(ctx, never()).next(any(Packet.class));
  }

  @Test
  public void withToken() {
    final InitialPacket initialPacket =
        p(of(retryToken.create(address, currentTimeMillis() + 10000)));
    handler.onReceivePacket(initialPacket, ctx);

    // no retry sent
    verify(ctx, never()).sendPacket(any(Packet.class));

    // initial packet propagated
    verify(ctx).next(initialPacket);
  }

  @Test
  public void withInvalidToken() {
    final InitialPacket initialPacket = p(of("this is not a token".getBytes()));
    handler.onReceivePacket(initialPacket, ctx);

    assertToken(initialPacket.getSourceConnectionId());

    // initial packet was not propagated
    verify(ctx, never()).next(any(Packet.class));
  }

  private void assertToken(final Optional<ConnectionId> expectedDestConnId) {
    final ArgumentCaptor<RetryPacket> retryCaptor = ArgumentCaptor.forClass(RetryPacket.class);
    verify(ctx).sendPacket(retryCaptor.capture());

    final RetryPacket retry = retryCaptor.getValue();
    assertEquals(expectedDestConnId, retry.getDestinationConnectionId());
    retryToken.validate(retry.getRetryToken(), address, currentTimeMillis() + 10000);
  }

  @Test
  public void verifyState() {
    // retry handler should only validate messages in the Started state.
    // TODO if this correct?
    when(ctx.getState()).thenReturn(State.BeforeHello);

    final InitialPacket initialPacket = p(Optional.empty());
    handler.onReceivePacket(initialPacket, ctx);

    // no retry packet sent
    verify(ctx, never()).sendPacket(any(RetryPacket.class));

    // initial instead packet propagated
    verify(ctx).next(initialPacket);
  }

  private InitialPacket p(final Optional<byte[]> token) {
    return InitialPacket.create(
        of(ConnectionId.random()),
        of(ConnectionId.random()),
        PacketNumber.MIN,
        Version.DRAFT_29,
        token,
        new PaddingFrame(1));
  }
}
