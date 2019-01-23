package com.protocol7.nettyquic.streams;

import static com.protocol7.nettyquic.streams.Stream.StreamType.Bidirectional;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.protocol7.nettyquic.connection.Connection;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.ResetStreamFrame;
import com.protocol7.nettyquic.protocol.frames.StreamFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StreamTest {

  public static final byte[] DATA = "Hello".getBytes();
  @Mock private Connection connection;
  @Mock private StreamListener listener;
  @Mock private FullPacket packet;
  private final StreamId streamId = StreamId.random(true, true);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(connection.getDestinationConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    when(connection.getSourceConnectionId()).thenReturn(Optional.of(ConnectionId.random()));
    when(connection.nextSendPacketNumber()).thenReturn(new PacketNumber(1));

    when(packet.getPacketNumber()).thenReturn(new PacketNumber(123));
    when(connection.sendPacket(any(Frame.class))).thenReturn(packet);
  }

  @Test
  public void write() {
    Stream stream = new Stream(streamId, connection, listener, Bidirectional, flowController);

    stream.write(DATA, false);

    StreamFrame frame = (StreamFrame) captureFrame();

    assertEquals(DATA, frame.getData());
    assertEquals(0, frame.getOffset());
    assertEquals(streamId, frame.getStreamId());
    assertFalse(frame.isFin());
  }

  @Test
  public void writeWithOffset() {
    Stream stream = new Stream(streamId, connection, listener, Bidirectional, flowController);

    stream.write(DATA, false);
    StreamFrame frame1 = (StreamFrame) captureFrame();
    assertEquals(0, frame1.getOffset());

    stream.write(DATA, false);
    StreamFrame frame2 = (StreamFrame) captureFrame();
    assertEquals(DATA.length, frame2.getOffset());
  }

  @Test
  public void reset() {
    Stream stream = new Stream(streamId, connection, listener, Bidirectional, flowController);

    stream.write(DATA, false);
    captureFrame();

    stream.reset(123);
    assertTrue(stream.isClosed());
    ResetStreamFrame frame2 = (ResetStreamFrame) captureFrame();

    assertEquals(streamId, frame2.getStreamId());
    assertEquals(123, frame2.getApplicationErrorCode());
    assertEquals(DATA.length, frame2.getOffset());
  }

  @Test(expected = IllegalStateException.class)
  public void resetOnClosed() {
    Stream stream = new Stream(streamId, connection, listener, Bidirectional, flowController);

    stream.reset(123);
    stream.reset(123);
  }

  @Test(expected = IllegalStateException.class)
  public void writeOnClosed() {
    Stream stream = new Stream(streamId, connection, listener, Bidirectional, flowController);
    stream.write(DATA, true);
    assertTrue(stream.isClosed());
    stream.write(DATA, true);
  }

  private Frame captureFrame() {
    ArgumentCaptor<Frame> packetCaptor = ArgumentCaptor.forClass(Frame.class);
    verify(connection, atLeastOnce()).sendPacket(packetCaptor.capture());
    return packetCaptor.getValue();
  }

  @Test
  public void onData() {
    Stream stream = new Stream(streamId, connection, listener, Bidirectional, flowController);
    stream.onData(0, true, DATA);

    verify(listener).onData(stream, DATA);
  }

  @Test
  public void onReset() {
    Stream stream = new Stream(streamId, connection, listener, Bidirectional, flowController);
    stream.onReset(123, 456);

    verify(listener).onReset(stream, 123, 456);

    assertTrue(stream.isClosed());
  }
}
