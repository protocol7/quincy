package com.protocol7.nettyquic.reliability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Payload;
import com.protocol7.nettyquic.protocol.frames.AckBlock;
import com.protocol7.nettyquic.protocol.frames.AckFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.PingFrame;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.ShortPacket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PacketBufferTest {

  @Mock private PipelineContext ctx;
  @Mock private AckDelay ackDelay;

  private PacketBuffer buffer;

  @Before
  public void setUp() {
    when(ctx.getState()).thenReturn(State.Ready);

    when(ackDelay.time()).thenReturn(123L);
    when(ackDelay.delay(anyLong())).thenReturn(45L);
    when(ackDelay.calculate(anyLong(), any(TimeUnit.class))).thenReturn(67L);

    buffer = new PacketBuffer(ackDelay);
  }

  @Test
  public void dontAckOnlyAcks() {
    Packet ackPacket = packet(1, new AckFrame(123, AckBlock.fromLongs(7, 8)));

    buffer.onReceivePacket(ackPacket, ctx);

    // should not send an ack
    verify(ctx, never()).send(any(Frame.class));
    assertBufferEmpty();

    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.onReceivePacket(pingPacket, ctx);

    AckFrame actual = (AckFrame) verifySent();

    assertEquals(AckBlock.fromLongs(1, 2), actual.getBlocks().get(0));
  }

  @Test
  public void ackOnPing() {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.onReceivePacket(pingPacket, ctx);

    AckFrame actual = (AckFrame) verifySent();
    assertEquals(67, actual.getAckDelay());
    assertEquals(AckBlock.fromLongs(2, 2), actual.getBlocks().get(0));
  }

  @Test
  public void send() {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.beforeSendPacket(pingPacket, ctx);

    Packet actual = verifyNext();

    assertEquals(pingPacket, actual);

    assertBuffered(2);
  }

  @Test
  public void ackSentPacket() {
    // send packet to buffer it

    Packet pingPacket = packet(2, PingFrame.INSTANCE);
    buffer.beforeSendPacket(pingPacket, ctx);
    assertBuffered(2);

    // now ack the packet
    Packet ackPacket = packet(3, new AckFrame(123, AckBlock.fromLongs(2, 2)));
    buffer.onReceivePacket(ackPacket, ctx);

    // all messages should now have been acked
    assertBufferEmpty();
  }

  private Packet packet(long pn, Frame... frames) {
    return new ShortPacket(
        false, Optional.of(ConnectionId.random()), new PacketNumber(pn), new Payload(frames));
  }

  private Packet verifyNext() {
    ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    verify(ctx).next(captor.capture());

    return captor.getValue();
  }

  private Frame verifySent() {
    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(ctx).send(captor.capture());

    return captor.getValue();
  }

  private void assertBuffered(long packetNumber) {
    // packet buffered for future acking
    assertTrue(buffer.getBuffer().containsKey(new PacketNumber(packetNumber)));
  }

  private void assertBufferEmpty() {
    assertTrue(buffer.getBuffer().isEmpty());
  }
}
