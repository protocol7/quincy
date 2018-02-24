package com.protocol7.nettyquick.streams;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.protocol7.nettyquick.connection.Connection;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.StreamId;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class StreamTest {

  public static final byte[] DATA = "Hello".getBytes();
  @Mock
  private Connection connection;
  @Mock private StreamListener listener;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    Mockito.when(connection.getConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    Mockito.when(connection.nextPacketNumber()).thenReturn(PacketNumber.random());
  }

  @Test
  public void write() {
    StreamId streamId = StreamId.create();
    Stream stream = new Stream(streamId,
                               connection,
                               listener);

    stream.write(DATA);

    StreamFrame frame = captureFrame();

    assertEquals(DATA, frame.getData());
    assertEquals(0, frame.getOffset());
    assertEquals(streamId, frame.getStreamId());
    assertFalse(frame.isFin());
  }

  @Test
  public void writeWithOffset() {
    StreamId streamId = StreamId.create();
    Stream stream = new Stream(streamId,
                               connection,
                               listener);

    stream.write(DATA);
    StreamFrame frame1 = captureFrame();
    assertEquals(0, frame1.getOffset());

    stream.write(DATA);
    StreamFrame frame2 = captureFrame();
    assertEquals(DATA.length, frame2.getOffset());
  }

  private StreamFrame captureFrame() {
    ArgumentCaptor<Packet> packetCaptor = ArgumentCaptor.forClass(Packet.class);
    verify(connection, atLeastOnce()).sendPacket(packetCaptor.capture());
    return (StreamFrame) packetCaptor.getValue().getPayload().getFrames().get(0);
  }

}