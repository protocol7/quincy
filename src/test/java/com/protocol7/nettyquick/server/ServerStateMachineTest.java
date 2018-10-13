package com.protocol7.nettyquick.server;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.ShortHeader;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.protocol.frames.PongFrame;
import com.protocol7.nettyquick.protocol.frames.RstStreamFrame;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import com.protocol7.nettyquick.server.ServerStateMachine.ServerState;
import com.protocol7.nettyquick.streams.Stream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ServerStateMachineTest {

  public static final byte[] DATA = "Hello".getBytes();
  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  @Mock
  private ServerConnection connection;
  @Mock private Stream stream;
  private ServerStateMachine stm;
  private PacketNumber packetNumber = PacketNumber.random();
  private StreamId streamId = StreamId.random(true, true);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    when(connection.getDestinationConnectionId()).thenReturn(Optional.of(destConnectionId));
    when(connection.getSourceConnectionId()).thenReturn(Optional.of(srcConnectionId));
    when(connection.getOrCreateStream(streamId)).thenReturn(stream);

    stm = new ServerStateMachine(connection);
  }

  @Test
  public void handshake() {
    assertEquals(ServerState.BeforeInitial, stm.getState());
    stm.processPacket(initialPacket());

    assertEquals(ServerState.Ready, stm.getState());
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
    assertEquals(ServerState.BeforeInitial, stm.getState());

    stm.processPacket(packet(new PingFrame()));

    // ignoring in unexpected state, nothing should happen
    verify(connection, never()).sendPacket(any(Frame.class));
    assertEquals(ServerState.BeforeInitial, stm.getState());
  }

  private void getReady() {
    stm.processPacket(initialPacket());
  }

  private Frame captureLastSentFrame() {
    ArgumentCaptor<Frame> sentPacketCaptor = ArgumentCaptor.forClass(Frame.class);
    verify(connection, atLeastOnce()).sendPacket(sentPacketCaptor.capture());
    return sentPacketCaptor.getValue();
  }

  private InitialPacket initialPacket() {
    return InitialPacket.create(
            Optional.of(destConnectionId),
            Optional.of(srcConnectionId),
            Optional.empty(),
            Collections.emptyList());
  }

  private Packet packet(Frame... frames) {
    return new ShortPacket(new ShortHeader(false,
                           Optional.of(destConnectionId),
                           incrPacketNumber(),
                           new ProtectedPayload(frames)));
  }

  private PacketNumber incrPacketNumber() {
    packetNumber = packetNumber.next();
    return packetNumber;
  }
}