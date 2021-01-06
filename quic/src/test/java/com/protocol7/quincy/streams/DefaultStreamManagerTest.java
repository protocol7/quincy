package com.protocol7.quincy.streams;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.ResetStreamFrame;
import com.protocol7.quincy.protocol.frames.StreamFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.tls.EncryptionLevel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultStreamManagerTest {

  private static final byte[] DATA1 = "hello".getBytes();
  private static final byte[] DATA2 = "world".getBytes();

  @Mock private PipelineContext ctx;
  @Mock private StreamHandler listener;
  @Mock private FullPacket packet;

  private DefaultStreamManager manager;

  @Before
  public void setUp() {
    when(ctx.send(any(EncryptionLevel.class), any(Frame.class))).thenReturn(packet);
    when(ctx.getState()).thenReturn(State.Done);
    when(packet.getPacketNumber()).thenReturn(456L);

    manager = new DefaultStreamManager(ctx, listener);
  }

  @Test
  public void streamSingleWrite() {
    final Stream stream = manager.openStream(true, true);

    stream.write(DATA1, true);
    verify(ctx)
        .send(any(EncryptionLevel.class), eq(new StreamFrame(stream.getId(), 0, true, DATA1)));

    assertTrue(stream.isFinished());
  }

  @Test
  public void streamMultiWrite() {
    final Stream stream = manager.openStream(true, true);

    stream.write(DATA1, false);
    verify(ctx)
        .send(any(EncryptionLevel.class), eq(new StreamFrame(stream.getId(), 0, false, DATA1)));

    assertFalse(stream.isFinished());

    stream.write(DATA2, true);
    verify(ctx)
        .send(
            any(EncryptionLevel.class),
            eq(new StreamFrame(stream.getId(), DATA1.length, true, DATA2)));

    assertTrue(stream.isFinished());
  }

  @Test
  public void streamReset() {
    final Stream stream = manager.openStream(true, true);

    stream.write(DATA1, false);
    verify(ctx)
        .send(any(EncryptionLevel.class), eq(new StreamFrame(stream.getId(), 0, false, DATA1)));

    stream.reset(123);

    verify(ctx)
        .send(
            any(EncryptionLevel.class),
            eq(new ResetStreamFrame(stream.getId(), 123, DATA1.length)));

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveSingle() {
    final Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, true, DATA1)), ctx);
    verify(listener).onData(stream, DATA1, true);

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveMulti() {
    final Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), ctx);
    verify(listener).onData(stream, DATA1, false);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), DATA1.length, true, DATA2)), ctx);
    verify(listener).onData(stream, DATA2, true);

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveMultiOutOfOrder() {
    final Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), DATA1.length, true, DATA2)), ctx);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), ctx);
    verify(listener).onData(stream, DATA1, false);
    verify(listener).onData(stream, DATA2, true);

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveReset() {
    final Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), ctx);
    verify(listener).onData(stream, DATA1, false);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new ResetStreamFrame(stream.getId(), 123, DATA1.length)), ctx);
    verifyNoMoreInteractions(listener);

    assertTrue(stream.isFinished());
  }

  @Test(expected = IllegalStateException.class)
  public void receiveInInvalidState() {
    when(ctx.getState()).thenReturn(State.BeforeHandshake);
    manager.onReceivePacket(p(new StreamFrame(0, 0, false, DATA1)), ctx);
  }

  private FullPacket p(final Frame... frames) {
    return ShortPacket.create(
        false, ConnectionId.random(), ConnectionId.random(), PacketNumber.MIN, frames);
  }
}
