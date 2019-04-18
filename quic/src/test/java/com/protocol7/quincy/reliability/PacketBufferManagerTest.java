package com.protocol7.quincy.reliability;

import static com.protocol7.quincy.protocol.ConnectionId.random;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import com.protocol7.quincy.FrameSender;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Payload;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.AckBlock;
import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.protocol.packets.HandshakePacket;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.utils.Ticker;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PacketBufferManagerTest {

  @Mock private PipelineContext ctx;
  @Mock private FrameSender frameSender;
  @Mock private AckDelay ackDelay;
  @Mock private Timer timer;
  @Mock private Ticker ticker;
  @Mock private Timeout timeout;

  private PacketBufferManager buffer;
  private TimerTask resendTask;

  @Before
  public void setUp() {
    when(ackDelay.time()).thenReturn(2000_0000_0000L);
    when(ackDelay.delay(anyLong())).thenReturn(45L);
    when(ackDelay.calculate(anyLong(), any(TimeUnit.class))).thenReturn(67L);

    when(ticker.nanoTime()).thenReturn(2000_0000_0000L);

    ArgumentCaptor<TimerTask> taskCaptor = ArgumentCaptor.forClass(TimerTask.class);
    when(timer.newTimeout(taskCaptor.capture(), anyLong(), any(TimeUnit.class))).thenReturn(null);

    when(timeout.timer()).thenReturn(timer);

    buffer = new PacketBufferManager(ackDelay, frameSender, timer, ticker);

    resendTask = taskCaptor.getValue();
  }

  @Test
  public void dontAckOnlyAcks() {
    Packet ackPacket = packet(1, new AckFrame(123, AckBlock.fromLongs(7, 8)));

    buffer.onReceivePacket(ackPacket, ctx);

    // should not send an ack
    verify(ctx, never()).send(any(Frame.class));
    assertBufferEmpty();

    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.onReceivePacket(pingPacket, ctx);

    AckFrame actual = (AckFrame) verifySent();

    assertEquals(AckBlock.fromLongs(1, 2), actual.getBlocks().get(0));
  }

  @Test
  public void ackOnPing() {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.onReceivePacket(pingPacket, ctx);

    AckFrame actual = (AckFrame) verifySent();
    assertEquals(67, actual.getAckDelay());
    assertEquals(AckBlock.fromLongs(2, 2), actual.getBlocks().get(0));
  }

  @Test
  public void ackInitial() {
    buffer.beforeSendPacket(ip(2, PingFrame.INSTANCE), ctx);

    assertTrue(buffer.getBuffer().isEmpty());
    assertTrue(buffer.getHandshakeBuffer().isEmpty());
    assertFalse(buffer.getInitialBuffer().isEmpty());

    buffer.onReceivePacket(ip(3, new AckFrame(123, new AckBlock(2, 2))), ctx);

    assertTrue(buffer.getInitialBuffer().isEmpty());
  }

  @Test
  public void ackInitialWithHandshake() {
    buffer.beforeSendPacket(ip(2, PingFrame.INSTANCE), ctx);

    // handshake packet implicitly acks any initial packets
    buffer.onReceivePacket(hp(3, PingFrame.INSTANCE), ctx);

    assertTrue(buffer.getInitialBuffer().isEmpty());
  }

  @Test
  public void ackInitialWithInvalidPacketType() {
    buffer.beforeSendPacket(ip(2, PingFrame.INSTANCE), ctx);

    buffer.onReceivePacket(packet(3, new AckFrame(123, new AckBlock(2, 2))), ctx);

    // must not be acked
    assertFalse(buffer.getInitialBuffer().isEmpty());
  }

  @Test
  public void ackHandshake() {
    buffer.beforeSendPacket(hp(2, PingFrame.INSTANCE), ctx);

    assertTrue(buffer.getBuffer().isEmpty());
    assertFalse(buffer.getHandshakeBuffer().isEmpty());
    assertTrue(buffer.getInitialBuffer().isEmpty());

    buffer.onReceivePacket(hp(3, new AckFrame(123, new AckBlock(2, 2))), ctx);

    assertTrue(buffer.getHandshakeBuffer().isEmpty());
  }

  @Test
  public void ackHandshakeWithShort() {
    buffer.beforeSendPacket(hp(2, PingFrame.INSTANCE), ctx);

    buffer.onReceivePacket(packet(3, PingFrame.INSTANCE), ctx);

    assertTrue(buffer.getHandshakeBuffer().isEmpty());
  }

  @Test
  public void ackHandshakeWithInvalidPacketType() {
    buffer.beforeSendPacket(hp(2, PingFrame.INSTANCE), ctx);

    buffer.onReceivePacket(ip(3, new AckFrame(123, new AckBlock(2, 2))), ctx);

    // must not be acked
    assertFalse(buffer.getHandshakeBuffer().isEmpty());
  }

  @Test
  public void ackPacket() {
    buffer.beforeSendPacket(packet(2, PingFrame.INSTANCE), ctx);

    assertFalse(buffer.getBuffer().isEmpty());
    assertTrue(buffer.getHandshakeBuffer().isEmpty());
    assertTrue(buffer.getInitialBuffer().isEmpty());

    buffer.onReceivePacket(packet(3, new AckFrame(123, new AckBlock(2, 2))), ctx);

    assertTrue(buffer.getBuffer().isEmpty());
  }

  @Test
  public void ackPacketWithInvalidPacketType() {
    buffer.beforeSendPacket(packet(2, PingFrame.INSTANCE), ctx);

    buffer.onReceivePacket(hp(3, new AckFrame(123, new AckBlock(2, 2))), ctx);

    // must not be acked
    assertFalse(buffer.getBuffer().isEmpty());
  }

  @Test
  public void send() {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);

    buffer.beforeSendPacket(pingPacket, ctx);

    Packet actual = verifyNext();

    assertEquals(pingPacket, actual);

    assertBuffered(2);
  }

  @Test
  public void resend() throws Exception {
    Packet pingPacket = packet(2, PingFrame.INSTANCE);
    buffer.beforeSendPacket(pingPacket, ctx);
    assertBuffered(2);

    // move time forward
    when(ticker.nanoTime()).thenReturn(3000_0000_0000L);

    resendTask.run(timeout);

    verify(frameSender).send(PingFrame.INSTANCE);
  }

  private Packet packet(long pn, Frame... frames) {
    return new ShortPacket(false, of(random()), new PacketNumber(pn), new Payload(frames));
  }

  private Packet ip(long pn, Frame... frames) {
    return InitialPacket.create(
        of(random()),
        of(random()),
        new PacketNumber(pn),
        Version.DRAFT_18,
        Optional.empty(),
        frames);
  }

  private Packet hp(long pn, Frame... frames) {
    return HandshakePacket.create(
        of(random()), of(random()), new PacketNumber(pn), Version.DRAFT_18, frames);
  }

  private Packet verifyNext() {
    ArgumentCaptor<Packet> captor = ArgumentCaptor.forClass(Packet.class);
    verify(ctx).next(captor.capture());

    return captor.getValue();
  }

  private Frame verifySent() {
    ArgumentCaptor<Frame> captor = ArgumentCaptor.forClass(Frame.class);
    verify(ctx).send(captor.capture());

    return captor.getValue();
  }

  private void assertBuffered(long packetNumber) {
    // packet buffered for future acking
    assertTrue(buffer.getBuffer().contains(packetNumber));
  }

  private void assertBufferEmpty() {
    assertTrue(buffer.getBuffer().isEmpty());
  }
}
