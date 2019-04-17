package com.protocol7.quincy.streams;

import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Payload;
import com.protocol7.quincy.protocol.StreamId;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.ResetStreamFrame;
import com.protocol7.quincy.protocol.frames.StreamFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.ShortPacket;
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
  @Mock private StreamListener listener;
  @Mock private FullPacket packet;

  private DefaultStreamManager manager;

  @Before
  public void setUp() {
    when(ctx.send(any(Frame.class))).thenReturn(packet);
    when(ctx.getState()).thenReturn(State.Ready);
    when(packet.getPacketNumber()).thenReturn(new PacketNumber(456));

    manager = new DefaultStreamManager(ctx, listener);
  }

  @Test
  public void streamSingleWrite() {
    Stream stream = manager.openStream(true, true);

    stream.write(DATA1, true);
    verify(ctx).send(new StreamFrame(stream.getId(), 0, true, DATA1));

    assertTrue(stream.isFinished());
  }

  @Test
  public void streamMultiWrite() {
    Stream stream = manager.openStream(true, true);

    stream.write(DATA1, false);
    verify(ctx).send(new StreamFrame(stream.getId(), 0, false, DATA1));

    assertFalse(stream.isFinished());

    stream.write(DATA2, true);
    verify(ctx).send(new StreamFrame(stream.getId(), DATA1.length, true, DATA2));

    assertTrue(stream.isFinished());
  }

  @Test
  public void streamReset() {
    Stream stream = manager.openStream(true, true);

    stream.write(DATA1, false);
    verify(ctx).send(new StreamFrame(stream.getId(), 0, false, DATA1));

    stream.reset(123);

    verify(ctx).send(new ResetStreamFrame(stream.getId(), 123, DATA1.length));

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveSingle() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, true, DATA1)), ctx);
    verify(listener).onData(stream, DATA1);
    verify(listener).onFinished();

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveMulti() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), ctx);
    verify(listener).onData(stream, DATA1);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), DATA1.length, true, DATA2)), ctx);
    verify(listener).onData(stream, DATA2);
    verify(listener).onFinished();

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveMultiOutOfOrder() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), DATA1.length, true, DATA2)), ctx);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), ctx);
    verify(listener).onData(stream, DATA1);
    verify(listener).onData(stream, DATA2);
    verify(listener).onFinished();

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveReset() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), ctx);
    verify(listener).onData(stream, DATA1);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new ResetStreamFrame(stream.getId(), 123, DATA1.length)), ctx);
    verify(listener).onReset(stream, 123, DATA1.length);
    verifyNoMoreInteractions(listener);

    assertTrue(stream.isFinished());
  }

  @Test(expected = IllegalStateException.class)
  public void receiveInInvalidState() {
    when(ctx.getState()).thenReturn(State.BeforeReady);
    manager.onReceivePacket(p(new StreamFrame(new StreamId(0), 0, false, DATA1)), ctx);
  }

  private FullPacket p(Frame... frames) {
    return new ShortPacket(false, of(ConnectionId.random()), PacketNumber.MIN, new Payload(frames));
  }
}
