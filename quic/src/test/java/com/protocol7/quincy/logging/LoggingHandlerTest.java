package com.protocol7.quincy.logging;

import static java.util.Optional.of;
import static org.mockito.Mockito.verify;

import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.PacketNumber;
import com.protocol7.quincy.protocol.Payload;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.ShortPacket;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LoggingHandlerTest {

  private final LoggingHandler handler = new LoggingHandler(true);
  @Mock private PipelineContext ctx;

  @Test
  public void receive() {
    Packet packet = p(PingFrame.INSTANCE);
    handler.onReceivePacket(packet, ctx);

    verify(ctx).next(packet);
  }

  @Test
  public void send() {
    Packet packet = p(PingFrame.INSTANCE);
    handler.beforeSendPacket(packet, ctx);

    verify(ctx).next(packet);
  }

  private FullPacket p(Frame... frames) {
    return new ShortPacket(false, of(ConnectionId.random()), PacketNumber.MIN, new Payload(frames));
  }
}
