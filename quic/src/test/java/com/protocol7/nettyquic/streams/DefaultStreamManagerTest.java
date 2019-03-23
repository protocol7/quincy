package com.protocol7.nettyquic.streams;

import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.protocol7.nettyquic.connection.FrameSender;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Payload;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.ResetStreamFrame;
import com.protocol7.nettyquic.protocol.frames.StreamFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.ShortPacket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DefaultStreamManagerTest {

  private static final byte[] DATA1 = "hello".getBytes();
  private static final byte[] DATA2 = "world".getBytes();

  @Mock private FrameSender sender;
  @Mock private StreamListener listener;
  @Mock private FullPacket packet;

  private DefaultStreamManager manager;

  @Before
  public void setUp() {
    initMocks(this);

    when(sender.send(any(Frame.class))).thenReturn(packet);
    when(packet.getPacketNumber()).thenReturn(new PacketNumber(456));

    manager = new DefaultStreamManager(sender, listener);
  }

  @Test
  public void streamSingleWrite() {
    Stream stream = manager.openStream(true, true);

    stream.write(DATA1, true);
    verify(sender).send(new StreamFrame(stream.getId(), 0, true, DATA1));

    assertTrue(stream.isFinished());
  }

  @Test
  public void streamMultiWrite() {
    Stream stream = manager.openStream(true, true);

    stream.write(DATA1, false);
    verify(sender).send(new StreamFrame(stream.getId(), 0, false, DATA1));

    assertFalse(stream.isFinished());

    stream.write(DATA2, true);
    verify(sender).send(new StreamFrame(stream.getId(), DATA1.length, true, DATA2));

    assertTrue(stream.isFinished());
  }

  @Test
  public void streamReset() {
    Stream stream = manager.openStream(true, true);

    stream.write(DATA1, false);
    verify(sender).send(new StreamFrame(stream.getId(), 0, false, DATA1));

    stream.reset(123);

    verify(sender).send(new ResetStreamFrame(stream.getId(), 123, DATA1.length));

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveSingle() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, true, DATA1)), sender);
    verify(listener).onData(stream, DATA1);
    verify(listener).onFinished();

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveMulti() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), sender);
    verify(listener).onData(stream, DATA1);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), DATA1.length, true, DATA2)), sender);
    verify(listener).onData(stream, DATA2);
    verify(listener).onFinished();

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveMultiOutOfOrder() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), DATA1.length, true, DATA2)), sender);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), sender);
    verify(listener).onData(stream, DATA1);
    verify(listener).onData(stream, DATA2);
    verify(listener).onFinished();

    assertTrue(stream.isFinished());
  }

  @Test
  public void receiveReset() {
    Stream stream = manager.openStream(true, true);

    manager.onReceivePacket(p(new StreamFrame(stream.getId(), 0, false, DATA1)), sender);
    verify(listener).onData(stream, DATA1);
    verifyNoMoreInteractions(listener);

    manager.onReceivePacket(p(new ResetStreamFrame(stream.getId(), 123, DATA1.length)), sender);
    verify(listener).onReset(stream, 123, DATA1.length);
    verifyNoMoreInteractions(listener);

    assertTrue(stream.isFinished());
  }

  private FullPacket p(Frame... frames) {
    return new ShortPacket(false, of(ConnectionId.random()), PacketNumber.MIN, new Payload(frames));
  }
}
