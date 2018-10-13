package com.protocol7.nettyquick.client;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.ShortHeader;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.protocol.frames.PongFrame;
import com.protocol7.nettyquick.protocol.frames.RstStreamFrame;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;
import com.protocol7.nettyquick.protocol.packets.HandshakePacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import com.protocol7.nettyquick.streams.Stream;
import io.netty.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ClientStateMachineTest {

  public static final byte[] DATA = "Hello".getBytes();
  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  @Mock private ClientConnection connection;
  @Mock private Stream stream;
  private ClientStateMachine stm;
  private PacketNumber packetNumber = PacketNumber.random();
  private StreamId streamId = StreamId.random(true, true);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(connection.getDestinationConnectionId()).thenReturn(Optional.of(destConnectionId));
    when(connection.getSourceConnectionId()).thenReturn(Optional.of(srcConnectionId));
    when(connection.getOrCreateStream(streamId)).thenReturn(stream);

    stm = new ClientStateMachine(connection);
  }

  @Test
  public void handshake() {
    assertEquals(ClientStateMachine.ClientState.BeforeInitial, stm.getState());

    Future<Void> handshakeFuture = stm.handshake();
    assertFalse(handshakeFuture.isDone());
    assertEquals(ClientStateMachine.ClientState.InitialSent, stm.getState());

    stm.processPacket(HandshakePacket.create(
            Optional.of(destConnectionId),
            Optional.of(srcConnectionId),
            PacketNumber.random(),
            Version.CURRENT));

    assertTrue(handshakeFuture.isDone());
    assertEquals(ClientStateMachine.ClientState.Ready, stm.getState());
  }

  @Test
  public void streamFrame() {
    getReady();

    stm.processPacket(packet(new StreamFrame(streamId, 123, false, DATA)));

    verify(stream).onData(123, false, DATA);
  }

  @Test
  public void resetStreamFrame() {
    getReady();

    stm.processPacket(packet(new RstStreamFrame(streamId, 123, 456)));

    verify(stream).onReset(123, 456);
  }

  @Test
  public void pingWithoutData() {
    getReady();

    stm.processPacket(packet(new PingFrame()));

    // nothing should happen
    verify(connection, never()).sendPacket(any(Frame.class));
  }

  @Test
  public void pingWithData() {
    getReady();

    stm.processPacket(packet(new PingFrame(DATA)));

    PongFrame pongFrame = (PongFrame) captureLastSentFrame();

    assertArrayEquals(DATA, pongFrame.getData());
  }

  @Test
  public void frameBeforeHandshake() {
    // not handshaking
    assertEquals(ClientStateMachine.ClientState.BeforeInitial, stm.getState());

    stm.processPacket(packet(new PingFrame()));

    // ignoring in unexpected state, nothing should happen
    verify(connection, never()).sendPacket(any(Frame.class));
    assertEquals(ClientStateMachine.ClientState.BeforeInitial, stm.getState());
  }


  private Frame captureLastSentFrame() {
    ArgumentCaptor<Frame> sentPacketCaptor = ArgumentCaptor.forClass(Frame.class);
    verify(connection, atLeastOnce()).sendPacket(sentPacketCaptor.capture());
    return sentPacketCaptor.getValue();
  }


  private Packet packet(Frame... frames) {
    return new ShortPacket(new ShortHeader(false,
                           false,
                           PacketType.Four_octets,
                           Optional.of(destConnectionId),
                           incrPacketNumber(),
                           new ProtectedPayload(frames)));
  }

  private PacketNumber incrPacketNumber() {
    packetNumber = packetNumber.next();
    return packetNumber;
  }

  private void getReady() {
    stm.handshake();
    stm.processPacket(HandshakePacket.create(
            Optional.of(destConnectionId),
            Optional.of(srcConnectionId),
            PacketNumber.random(),
            Version.CURRENT));
  }

}