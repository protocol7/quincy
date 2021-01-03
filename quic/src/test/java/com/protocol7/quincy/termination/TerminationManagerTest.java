package com.protocol7.quincy.termination;

import static com.protocol7.quincy.protocol.ConnectionId.random;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.protocol7.quincy.MockTimer;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.InternalConnection;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TerminationManagerTest {

  @Mock private InternalConnection connection;
  @Mock private PipelineContext ctx;

  private TerminationManager manager;
  private MockTimer timer = new MockTimer();

  @Before
  public void setUp() {
    manager = new TerminationManager(connection, timer, 123, TimeUnit.SECONDS);
  }

  @Test
  public void testConnectionCloseFrame() {
    final Packet packet = packet(new ConnectionCloseFrame(456, FrameType.PADDING, "Test"));

    manager.onReceivePacket(packet, ctx);

    verify(connection).closeByPeer();
    verify(ctx).setState(State.Closing);
    verify(ctx).setState(State.Closed);
    verify(ctx).next(packet);
  }

  @Test
  public void idleTimeout() throws Exception {
    assertTrue(timer.timeouts.isEmpty());

    manager.onReceivePacket(packet(PingFrame.INSTANCE), ctx);

    // timer set for next packet
    assertFalse(timer.timeouts.isEmpty());

    // now trigger timer
    timer.trigger();

    verify(connection).close(eq(TransportError.NO_ERROR), eq(FrameType.PADDING), anyString());
  }

  private Packet packet(final Frame... frames) {
    return ShortPacket.create(false, random(), random(), PacketNumber.MIN, frames);
  }
}
