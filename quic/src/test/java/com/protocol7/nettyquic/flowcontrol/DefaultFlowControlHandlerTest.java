package com.protocol7.nettyquic.flowcontrol;

import static java.util.Optional.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Payload;
import com.protocol7.nettyquic.protocol.StreamId;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.DataBlockedFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.MaxDataFrame;
import com.protocol7.nettyquic.protocol.frames.MaxStreamDataFrame;
import com.protocol7.nettyquic.protocol.frames.StreamDataBlockedFrame;
import com.protocol7.nettyquic.protocol.frames.StreamFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.ShortPacket;
import org.junit.Test;

public class DefaultFlowControlHandlerTest {

  private FlowControlManager handler = new DefaultFlowControlHandler(15, 10);
  private FrameSender sender = mock(FrameSender.class);
  private StreamId sid = new StreamId(123);
  private StreamId sid2 = new StreamId(456);

  @Test
  public void tryConsume() {
    assertTrue(handler.tryConsume(sid, 10, sender));
    verifyZeroInteractions(sender);

    // blocked on stream limit
    assertFalse(handler.tryConsume(sid, 11, sender));
    verify(sender).send(new StreamDataBlockedFrame(sid, 10));
  }

  @Test
  public void tryConsumeRefillStream() {
    assertTrue(handler.tryConsume(sid, 10, sender));
    verifyZeroInteractions(sender);

    // running out of stream tokens
    assertFalse(handler.tryConsume(sid, 12, sender));
    verify(sender).send(new StreamDataBlockedFrame(sid, 10));

    // increase stream tokens
    handler.onReceivePacket(p(new MaxStreamDataFrame(sid, 12)), sender);

    // we can now consumer stream tokens
    assertTrue(handler.tryConsume(sid, 12, sender));
    verifyZeroInteractions(sender);

    // but not this many
    assertFalse(handler.tryConsume(sid, 13, sender));
    verify(sender).send(new StreamDataBlockedFrame(sid, 12));

    // must not send any additional data blocked frames until new size
    assertFalse(handler.tryConsume(sid, 13, sender));
    verifyZeroInteractions(sender);

    // reset data blocked frames
    handler.onReceivePacket(p(new MaxStreamDataFrame(sid, 13)), sender);

    // we must now get a new data blocked frame
    assertFalse(handler.tryConsume(sid, 14, sender));
    verify(sender).send(new StreamDataBlockedFrame(sid, 13));
  }

  @Test
  public void tryConsumeRefillConnection() {
    assertTrue(handler.tryConsume(sid, 10, sender));
    verifyZeroInteractions(sender);

    assertFalse(handler.tryConsume(sid2, 6, sender));
    verify(sender).send(new DataBlockedFrame(15));

    handler.onReceivePacket(p(new MaxDataFrame(16)), sender);

    assertTrue(handler.tryConsume(sid2, 6, sender));
    verifyZeroInteractions(sender);

    assertFalse(handler.tryConsume(sid2, 7, sender));
    verify(sender).send(new DataBlockedFrame(16));

    // must not send any additional data blocked frames until new size
    assertFalse(handler.tryConsume(sid2, 7, sender));
    verifyZeroInteractions(sender);

    // reset data blocked frames
    handler.onReceivePacket(p(new MaxDataFrame(17)), sender);

    // we must now get a new data blocked frame
    assertFalse(handler.tryConsume(sid2, 8, sender));
    verify(sender).send(new DataBlockedFrame(17));
  }

  @Test
  public void streamFrames() {
    handler.onReceivePacket(p(new StreamFrame(sid, 0, false, new byte[3])), sender);
    verifyZeroInteractions(sender);

    // going over 50% of the max stream offset, send a new max stream offset
    handler.onReceivePacket(p(new StreamFrame(sid, 3, false, new byte[3])), sender);
    verify(sender).send(new MaxStreamDataFrame(sid, 20));

    // going over 50% of the max connection offset, send a new max connection offset
    handler.onReceivePacket(p(new StreamFrame(sid, 6, false, new byte[3])), sender);
    verify(sender).send(new MaxDataFrame(30));

    // user more than flow control allow, must close connection
    handler.onReceivePacket(p(new StreamFrame(sid, 10, false, new byte[11])), sender);
    verify(sender).closeConnection(TransportError.FLOW_CONTROL_ERROR);
  }

  private FullPacket p(Frame frame) {
    return new ShortPacket(false, of(ConnectionId.random()), PacketNumber.MIN, new Payload(frame));
  }
}
