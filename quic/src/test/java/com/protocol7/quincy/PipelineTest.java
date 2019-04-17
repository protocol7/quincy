package com.protocol7.quincy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.protocol.packets.Packet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PipelineTest {

  private static class PropagatingInboundHandler implements InboundHandler {
    @Override
    public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
      ctx.next(packet);
    }
  }

  private static class NonPropagatingInboundHandler implements InboundHandler {
    @Override
    public void onReceivePacket(final Packet packet, final PipelineContext ctx) {}
  }

  private static class PropagatingOutboundHandler implements OutboundHandler {
    @Override
    public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {
      ctx.next(packet);
    }
  }

  private static class NonPropagatingOutboundHandler implements OutboundHandler {
    @Override
    public void beforeSendPacket(final Packet packet, final PipelineContext ctx) {}
  }

  @Mock Connection connection;
  @Mock Packet packet;

  @Test
  public void inboundPropagation() {
    InboundHandler handler1 = spy(new PropagatingInboundHandler());
    InboundHandler handler2 = spy(new PropagatingInboundHandler());
    InboundHandler handler3 = spy(new PropagatingInboundHandler());

    Pipeline pipeline = new Pipeline(List.of(handler1, handler2, handler3), List.of());

    pipeline.onPacket(connection, packet);

    verify(handler1).onReceivePacket(eq(packet), any(PipelineContext.class));
    verify(handler2).onReceivePacket(eq(packet), any(PipelineContext.class));
    verify(handler3).onReceivePacket(eq(packet), any(PipelineContext.class));
  }

  @Test
  public void inboundNonPropagation() {
    InboundHandler handler1 = spy(new PropagatingInboundHandler());
    InboundHandler handler2 = spy(new NonPropagatingInboundHandler());
    InboundHandler handler3 = spy(new PropagatingInboundHandler());

    Pipeline pipeline = new Pipeline(List.of(handler1, handler2, handler3), List.of());

    pipeline.onPacket(connection, packet);

    verify(handler1).onReceivePacket(eq(packet), any(PipelineContext.class));
    verify(handler2).onReceivePacket(eq(packet), any(PipelineContext.class));
    verifyZeroInteractions(handler3);
  }

  @Test
  public void outboundPropagation() {
    OutboundHandler handler1 = spy(new PropagatingOutboundHandler());
    OutboundHandler handler2 = spy(new PropagatingOutboundHandler());
    OutboundHandler handler3 = spy(new PropagatingOutboundHandler());

    Pipeline pipeline = new Pipeline(List.of(), List.of(handler1, handler2, handler3));

    pipeline.send(connection, packet);

    verify(handler1).beforeSendPacket(eq(packet), any(PipelineContext.class));
    verify(handler2).beforeSendPacket(eq(packet), any(PipelineContext.class));
    verify(handler3).beforeSendPacket(eq(packet), any(PipelineContext.class));
  }

  @Test
  public void outboundNonPropagation() {
    OutboundHandler handler1 = spy(new PropagatingOutboundHandler());
    OutboundHandler handler2 = spy(new NonPropagatingOutboundHandler());
    OutboundHandler handler3 = spy(new PropagatingOutboundHandler());

    Pipeline pipeline = new Pipeline(List.of(), List.of(handler1, handler2, handler3));

    pipeline.send(connection, packet);

    verify(handler1).beforeSendPacket(eq(packet), any(PipelineContext.class));
    verify(handler2).beforeSendPacket(eq(packet), any(PipelineContext.class));
    verifyZeroInteractions(handler3);
  }
}
