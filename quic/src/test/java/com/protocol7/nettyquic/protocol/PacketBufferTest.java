package com.protocol7.nettyquic.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.client.ClientState;
import com.protocol7.nettyquic.connection.Connection;
import com.protocol7.nettyquic.protocol.frames.AckBlock;
import com.protocol7.nettyquic.protocol.frames.AckFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.PingFrame;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.ShortPacket;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PacketBufferTest {

  @Mock private Connection connection;
  @Mock private PipelineContext ctx;

  private PacketBuffer buffer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(connection.getRemoteConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    when(connection.getLocalConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    when(connection.nextSendPacketNumber()).thenReturn(new PacketNumber(3));

    when(ctx.getState()).thenReturn(ClientState.Ready);

    buffer = new PacketBuffer(connection);
  }

  private Packet packet(long pn, Frame... frames) {
    return new ShortPacket(
        false, Optional.of(ConnectionId.random()), new PacketNumber(pn), new Payload(frames));
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

  @Test
  public void ackOnPing() {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.onReceivePacket(pingPacket, ctx);

    AckFrame actual = (AckFrame) verifySent();

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

  private void assertBuffered(long packetNumber) {
    // packet buffered for future acking
    assertTrue(buffer.getBuffer().containsKey(new PacketNumber(packetNumber)));
  }

  private void assertBufferEmpty() {
    assertTrue(buffer.getBuffer().isEmpty());
  }
}
