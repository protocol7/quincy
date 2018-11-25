package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.connection.Sender;
import com.protocol7.nettyquick.protocol.frames.AckBlock;
import com.protocol7.nettyquick.protocol.frames.AckFrame;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PacketBufferTest {

  @Mock private Connection connection;
  @Mock private Sender sender;

  private PacketBuffer buffer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(connection.getDestinationConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    when(connection.getSourceConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    when(connection.nextSendPacketNumber()).thenReturn(new PacketNumber(3));

    buffer = new PacketBuffer(connection, sender, pn -> {});
  }

  private Packet packet(long pn, Frame... frames) {
    return new ShortPacket(
        new ShortHeader(
            false, Optional.of(ConnectionId.random()), new PacketNumber(pn), new Payload(frames)));
  }

  @Test
  public void dontAckOnlyAcks() {
    Packet ackPacket = packet(1, new AckFrame(123, AckBlock.fromLongs(7, 8)));

    buffer.onPacket(ackPacket);

    // should not send an ack
    verifyNoMoreInteractions(connection);
    assertBufferEmpty();

    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.onPacket(pingPacket);

    // Packet actual = verifySent();

    // AckFrame actualFrame = (AckFrame) ((FullPacket)actual).getPayload().getFrames().get(0);
    // assertEquals(AckBlock.fromLongs(1, 2), actualFrame.getBlocks().get(0));

    // assertBuffered(3);
  }

  private Packet verifySent() {
    ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    verify(sender).send(captor.capture());

    return captor.getValue();
  }

  @Test
  public void ackOnPing() {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.onPacket(pingPacket);

    // Packet actual = verifySent();

    // AckFrame actualFrame = (AckFrame) ((FullPacket)actual).getPayload().getFrames().get(0);
    // assertEquals(AckBlock.fromLongs(2, 2), actualFrame.getBlocks().get(0));

    // assertBuffered(3);
  }

  @Test
  public void send() {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.send(pingPacket);

    Packet actual = verifySent();

    assertEquals(pingPacket, actual);

    assertBuffered(2);
  }

  @Test
  public void ackSentPacket() {
    // send packet to buffer it

    Packet pingPacket = packet(2, PingFrame.INSTANCE);
    buffer.send(pingPacket);
    assertBuffered(2);

    // now ack the packet
    Packet ackPacket = packet(3, new AckFrame(123, AckBlock.fromLongs(2, 2)));
    buffer.onPacket(ackPacket);

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
