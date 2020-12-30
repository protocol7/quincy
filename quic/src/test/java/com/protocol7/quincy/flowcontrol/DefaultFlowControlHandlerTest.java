package com.protocol7.quincy.flowcontrol;

import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Payload;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.DataBlockedFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.frames.MaxDataFrame;
import com.protocol7.quincy.protocol.frames.MaxStreamDataFrame;
import com.protocol7.quincy.protocol.frames.StreamDataBlockedFrame;
import com.protocol7.quincy.protocol.frames.StreamFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import com.protocol7.quincy.tls.EncryptionLevel;
import org.junit.Test;

public class DefaultFlowControlHandlerTest {

  private final DefaultFlowControlHandler handler = new DefaultFlowControlHandler(15, 10);
  private final PipelineContext ctx = mock(PipelineContext.class);
  private final long sid = 123;
  private final long sid2 = 456;

  @Test
  public void tryConsume() {
    assertTrue(handler.tryConsume(sid, 10, ctx));
    verifyZeroInteractions(ctx);

    // blocked on stream limit
    assertFalse(handler.tryConsume(sid, 11, ctx));
    verify(ctx).send(EncryptionLevel.OneRtt, new StreamDataBlockedFrame(sid, 10));
  }

  @Test
  public void tryConsumeRefillStream() {
    assertTrue(handler.tryConsume(sid, 10, ctx));
    verifyZeroInteractions(ctx);

    // running out of stream tokens
    assertFalse(handler.tryConsume(sid, 12, ctx));
    verify(ctx).send(EncryptionLevel.OneRtt, new StreamDataBlockedFrame(sid, 10));

    // increase stream tokens
    Packet packet = p(new MaxStreamDataFrame(sid, 12));
    handler.onReceivePacket(packet, ctx);
    verify(ctx).next(packet);

    // we can now consumer stream tokens
    assertTrue(handler.tryConsume(sid, 12, ctx));
    verifyZeroInteractions(ctx);

    // but not this many
    assertFalse(handler.tryConsume(sid, 13, ctx));
    verify(ctx).send(EncryptionLevel.OneRtt, new StreamDataBlockedFrame(sid, 12));

    // must not send any additional data blocked frames until new size
    assertFalse(handler.tryConsume(sid, 13, ctx));
    verifyZeroInteractions(ctx);

    // reset data blocked frames
    packet = p(new MaxStreamDataFrame(sid, 13));
    handler.onReceivePacket(packet, ctx);
    verify(ctx).next(packet);

    // we must now get a new data blocked frame
    assertFalse(handler.tryConsume(sid, 14, ctx));
    verify(ctx).send(EncryptionLevel.OneRtt, new StreamDataBlockedFrame(sid, 13));
  }

  @Test
  public void tryConsumeRefillConnection() {
    assertTrue(handler.tryConsume(sid, 10, ctx));
    verifyZeroInteractions(ctx);

    assertFalse(handler.tryConsume(sid2, 6, ctx));
    verify(ctx).send(EncryptionLevel.OneRtt, new DataBlockedFrame(15));

    Packet packet = p(new MaxDataFrame(16));
    handler.onReceivePacket(packet, ctx);
    verify(ctx).next(packet);

    assertTrue(handler.tryConsume(sid2, 6, ctx));
    verifyZeroInteractions(ctx);

    assertFalse(handler.tryConsume(sid2, 7, ctx));
    verify(ctx).send(EncryptionLevel.OneRtt, new DataBlockedFrame(16));

    // must not send any additional data blocked frames until new size
    assertFalse(handler.tryConsume(sid2, 7, ctx));
    verifyZeroInteractions(ctx);

    // reset data blocked frames
    packet = p(new MaxDataFrame(17));
    handler.onReceivePacket(packet, ctx);
    verify(ctx).next(packet);

    // we must now get a new data blocked frame
    assertFalse(handler.tryConsume(sid2, 8, ctx));
    verify(ctx).send(EncryptionLevel.OneRtt, new DataBlockedFrame(17));
  }

  @Test
  public void streamFrames() {
    Packet packet = p(new StreamFrame(sid, 0, false, new byte[3]));
    handler.onReceivePacket(packet, ctx);
    verify(ctx).next(packet);

    // going over 50% of the max stream offset, send a new max stream offset
    packet = p(new StreamFrame(sid, 3, false, new byte[3]));
    handler.onReceivePacket(packet, ctx);
    verify(ctx).send(EncryptionLevel.OneRtt, new MaxStreamDataFrame(sid, 20));
    verify(ctx).next(packet);

    // going over 50% of the max connection offset, send a new max connection offset
    packet = p(new StreamFrame(sid, 6, false, new byte[3]));
    handler.onReceivePacket(packet, ctx);
    verify(ctx).send(EncryptionLevel.OneRtt, new MaxDataFrame(30));
    verify(ctx).next(packet);

    // user more than flow control allow, must close connection
    packet = p(new StreamFrame(sid, 10, false, new byte[11]));
    handler.onReceivePacket(packet, ctx);
    verify(ctx)
        .closeConnection(eq(TransportError.FLOW_CONTROL_ERROR), eq(FrameType.STREAM), anyString());
    verify(ctx).next(packet);
  }

  private FullPacket p(final Frame frame) {
    return new ShortPacket(false, of(ConnectionId.random()), PacketNumber.MIN, new Payload(frame));
  }
}
