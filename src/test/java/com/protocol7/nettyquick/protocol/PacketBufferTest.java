package com.protocol7.nettyquick.protocol;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import com.protocol7.nettyquick.Connection;
import com.protocol7.nettyquick.protocol.frames.AckBlock;
import com.protocol7.nettyquick.protocol.frames.AckFrame;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PacketBufferTest {

  @Mock
  private Connection connection;

  private PacketBuffer buffer;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(connection.getConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    when(connection.nextPacketNumber()).thenReturn(new PacketNumber(3));

    buffer = new PacketBuffer(connection);
  }

  private Packet packet(long pn, Frame... frames) {
    return new ShortPacket(false,
                           false,
                           PacketType.Four_octets,
                           Optional.of(ConnectionId.random()),
                           new PacketNumber(pn),
                           new Payload(frames));
  }

  @Test
  public void dontAckOnlyAcks() {
    Packet ackPacket = packet(1, new AckFrame(123, AckBlock.fromLongs(7, 8)));

    buffer.onPacket(ackPacket);

    // should not send an ack
    verifyNoMoreInteractions(connection);
    assertBufferEmpty();

    Packet pingPacket = packet(2, new PingFrame("Hello".getBytes()));

    buffer.onPacket(pingPacket);

    ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    verify(connection).sendPacket(captor.capture());

    Packet actual = captor.getValue();

    AckFrame actualFrame = (AckFrame) actual.getPayload().getFrames().get(0);
    assertEquals(AckBlock.fromLongs(1, 2), actualFrame.getBlocks().get(0));

    assertBuffered(3);
  }

  @Test
  public void ackOnPing() {
    Packet pingPacket = packet(2, new PingFrame("Hello".getBytes()));

    buffer.onPacket(pingPacket);

    ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    verify(connection).sendPacket(captor.capture());

    Packet actual = captor.getValue();

    AckFrame actualFrame = (AckFrame) actual.getPayload().getFrames().get(0);
    assertEquals(AckBlock.fromLongs(2, 2), actualFrame.getBlocks().get(0));

    assertBuffered(3);
  }

  @Test
  public void send() {
    Packet pingPacket = packet(2, new PingFrame("Hello".getBytes()));

    buffer.send(pingPacket);

    ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    verify(connection).sendPacket(captor.capture());

    Packet actual = captor.getValue();

    assertEquals(pingPacket, actual);

    assertBuffered(2);
  }

  @Test
  public void ackSentPacket() {
    // send packet to buffer it

    Packet pingPacket = packet(2, new PingFrame("Hello".getBytes()));
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