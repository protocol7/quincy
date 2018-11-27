package com.protocol7.nettyquick.server;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.protocol.packets.ShortPacket;
import com.protocol7.nettyquick.server.ServerStateMachine.ServerState;
import com.protocol7.nettyquick.streams.Stream;
import java.util.Optional;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Ignore
public class ServerStateMachineTest {

  public static final byte[] DATA = "Hello".getBytes();
  private final ConnectionId destConnectionId = ConnectionId.random();
  private final ConnectionId srcConnectionId = ConnectionId.random();
  @Mock private ServerConnection connection;
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
    stm.processPacket(initialPacket(new CryptoFrame(0, "ch".getBytes())));

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

    stm.processPacket(packet(new ResetStreamFrame(streamId, 123, 456)));

    verify(stream).onReset(123, 456);
  }

  @Test
  public void pingWithoutData() {
    getReady();

    stm.processPacket(packet(PingFrame.INSTANCE));

    // nothing should happen
    verify(connection, never()).sendPacket(any(Frame.class));
  }

  @Test
  public void frameBeforeHandshake() {
    // not handshaking
    assertEquals(ServerState.BeforeInitial, stm.getState());

    stm.processPacket(initialPacket(new CryptoFrame(0, "ch".getBytes())));

    // ignoring in unexpected state, nothing should happen
    verify(connection, never()).sendPacket(any(Frame.class));
    assertEquals(ServerState.Ready, stm.getState());
  }

  private void getReady() {
    stm.processPacket(initialPacket(new CryptoFrame(0, "ch".getBytes())));
  }

  private Frame captureLastSentFrame() {
    ArgumentCaptor<Frame> sentPacketCaptor = ArgumentCaptor.forClass(Frame.class);
    verify(connection, atLeastOnce()).sendPacket(sentPacketCaptor.capture());
    return sentPacketCaptor.getValue();
  }

  private InitialPacket initialPacket(Frame... frames) {
    return InitialPacket.create(
        Optional.of(destConnectionId),
        Optional.of(srcConnectionId),
        Optional.empty(),
        Lists.newArrayList(frames));
  }

  private Packet packet(Frame... frames) {
    return new ShortPacket(
        new ShortHeader(
            false, Optional.of(destConnectionId), incrPacketNumber(), new Payload(frames)));
  }

  private PacketNumber incrPacketNumber() {
    packetNumber = packetNumber.next();
    return packetNumber;
  }
}
