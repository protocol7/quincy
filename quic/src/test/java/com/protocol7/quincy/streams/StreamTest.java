package com.protocol7.quincy.streams;

import static com.protocol7.quincy.streams.StreamType.Bidirectional;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.StreamId;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.ResetStreamFrame;
import com.protocol7.quincy.protocol.frames.StreamFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.tls.EncryptionLevel;
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
  private final long streamId = StreamId.next(-1, true, true);

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(packet.getPacketNumber()).thenReturn(123L);
    when(ctx.send(any(EncryptionLevel.class), any(Frame.class))).thenReturn(packet);
  }

  @Test
  public void write() {
    final DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.write(DATA, false);

    final StreamFrame frame = (StreamFrame) captureFrame();

    assertEquals(DATA, frame.getData());
    assertEquals(0, frame.getOffset());
    assertEquals(streamId, frame.getStreamId());
    assertFalse(frame.isFin());
  }

  @Test
  public void writeWithOffset() {
    final DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.write(DATA, false);
    final StreamFrame frame1 = (StreamFrame) captureFrame();
    assertEquals(0, frame1.getOffset());

    stream.write(DATA, false);
    final StreamFrame frame2 = (StreamFrame) captureFrame();
    assertEquals(DATA.length, frame2.getOffset());
  }

  @Test
  public void reset() {
    final DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.write(DATA, false);
    captureFrame();

    stream.reset(123);
    assertTrue(stream.isFinished());
    final ResetStreamFrame frame2 = (ResetStreamFrame) captureFrame();

    assertEquals(streamId, frame2.getStreamId());
    assertEquals(123, frame2.getApplicationErrorCode());
    assertEquals(DATA.length, frame2.getFinalSize());
  }

  @Test(expected = IllegalStateException.class)
  public void resetOnClosed() {
    final DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);

    stream.reset(123);
    stream.reset(123);
  }

  @Test(expected = IllegalStateException.class)
  public void writeOnClosed() {
    final DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);
    stream.write(DATA, true);
    assertTrue(stream.isFinished());
    stream.write(DATA, true);
  }

  private Frame captureFrame() {
    final ArgumentCaptor<Frame> packetCaptor = ArgumentCaptor.forClass(Frame.class);
    verify(ctx, atLeastOnce()).send(any(EncryptionLevel.class), packetCaptor.capture());
    return packetCaptor.getValue();
  }

  @Test
  public void onData() {
    final DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);
    stream.onData(0, true, DATA);

    verify(listener).onData(stream, DATA, true);
  }

  @Test
  public void onReset() {
    final DefaultStream stream = new DefaultStream(streamId, ctx, listener, Bidirectional);
    stream.onReset(123, 456);

    assertTrue(stream.isFinished());
  }
}
