package com.protocol7.nettyquic.streams;

import static com.protocol7.nettyquic.streams.StreamType.Bidirectional;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.ResetStreamFrame;
import com.protocol7.nettyquic.protocol.frames.StreamFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class StreamTest {

  public static final byte[] DATA = "Hello".getBytes();
  @Mock private PipelineContext ctx;
  @Mock private StreamListener listener;
  @Mock private FullPacket packet;
  private final StreamId streamId = StreamId.random(true, true);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(packet.getPacketNumber()).thenReturn(new PacketNumber(123));
    when(ctx.send(any(Frame.class))).thenReturn(packet);
  }

  @Test
  public void write() {
    DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.write(DATA, false);

    StreamFrame frame = (StreamFrame) captureFrame();

    assertEquals(DATA, frame.getData());
    assertEquals(0, frame.getOffset());
    assertEquals(streamId, frame.getStreamId());
    assertFalse(frame.isFin());
  }

  @Test
  public void writeWithOffset() {
    DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.write(DATA, false);
    StreamFrame frame1 = (StreamFrame) captureFrame();
    assertEquals(0, frame1.getOffset());

    stream.write(DATA, false);
    StreamFrame frame2 = (StreamFrame) captureFrame();
    assertEquals(DATA.length, frame2.getOffset());
  }

  @Test
  public void reset() {
    DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.write(DATA, false);
    captureFrame();

    stream.reset(123);
    assertTrue(stream.isFinished());
    ResetStreamFrame frame2 = (ResetStreamFrame) captureFrame();

    assertEquals(streamId, frame2.getStreamId());
    assertEquals(123, frame2.getApplicationErrorCode());
    assertEquals(DATA.length, frame2.getOffset());
  }

  @Test(expected = IllegalStateException.class)
  public void resetOnClosed() {
    DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.reset(123);
    stream.reset(123);
  }

  @Test(expected = IllegalStateException.class)
  public void writeOnClosed() {
    DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);
    stream.write(DATA, true);
    assertTrue(stream.isFinished());
    stream.write(DATA, true);
  }

  private Frame captureFrame() {
    ArgumentCaptor<Frame> packetCaptor = ArgumentCaptor.forClass(Frame.class);
    verify(ctx, atLeastOnce()).send(packetCaptor.capture());
    return packetCaptor.getValue();
  }

  @Test
  public void onData() {
    DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);
    stream.onData(0, true, DATA);

    verify(listener).onData(stream, DATA);
  }

  @Test
  public void onReset() {
    DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);
    stream.onReset(123, 456);

    verify(listener).onReset(stream, 123, 456);

    assertTrue(stream.isFinished());
  }
}
