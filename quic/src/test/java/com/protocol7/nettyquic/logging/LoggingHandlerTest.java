package com.protocol7.nettyquic.logging;

import static java.util.Optional.of;
import static org.mockito.Mockito.verify;

import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.Payload;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.PingFrame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.ShortPacket;
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
