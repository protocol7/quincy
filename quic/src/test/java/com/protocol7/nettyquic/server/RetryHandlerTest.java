package com.protocol7.nettyquic.server;

import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Payload;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.protocol.frames.PingFrame;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.RetryPacket;
import com.protocol7.nettyquic.utils.Rnd;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class RetryHandlerTest {

  @Mock PipelineContext ctx;
  private RetryHandler handler = new RetryHandler();

  @Before
  public void setUp() {
    initMocks(this);

    when(ctx.getVersion()).thenReturn(Version.DRAFT_18);
  }

  @Test
  public void retry() {
    InitialPacket initialPacket = p(Optional.empty());
    handler.onReceivePacket(initialPacket, ctx);

    ArgumentCaptor<RetryPacket> retryCaptor = ArgumentCaptor.forClass(RetryPacket.class);

    verify(ctx).sendPacket(retryCaptor.capture());

    RetryPacket retry = retryCaptor.getValue();

    assertEquals(initialPacket.getSourceConnectionId(), retry.getDestinationConnectionId());
    // TODO verify token

    // initial packet was not propagated
    verify(ctx, never()).next(any(Packet.class));
  }

  @Test
  public void retryExists() {
    InitialPacket initialPacket = p(of(Rnd.rndBytes(20)));
    handler.onReceivePacket(initialPacket, ctx);

    // no retry sent
    verify(ctx, never()).sendPacket(any(Packet.class));

    // initial packet propagated
    verify(ctx).next(initialPacket);
  }

  private InitialPacket p(Optional<byte[]> token) {
    return new InitialPacket(
        of(ConnectionId.random()),
        of(ConnectionId.random()),
        Version.DRAFT_18,
        PacketNumber.MIN,
        new Payload(PingFrame.INSTANCE),
        token);
  }
}
