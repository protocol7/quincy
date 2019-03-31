package com.protocol7.nettyquic;

import com.protocol7.nettyquic.connection.Connection;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.frames.FrameType;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import java.util.Iterator;
import java.util.List;

public class Pipeline {

  private final List<InboundHandler> inboundHandlers;
  private final List<OutboundHandler> outboundHandlers;

  public Pipeline(
      final List<InboundHandler> inboundHandlers, final List<OutboundHandler> outboundHandlers) {
    this.inboundHandlers = inboundHandlers;
    this.outboundHandlers = outboundHandlers;
  }

  public void onPacket(Connection connection, Packet packet) {
    final Iterator<InboundHandler> iter = inboundHandlers.iterator();

    PipelineContext ctx =
        new PipelineContext() {
          @Override
          public void next(final Packet newPacket) {
            if (iter.hasNext()) {
              final InboundHandler handler = iter.next();
              handler.onReceivePacket(newPacket, this);
            }
          }

          @Override
          public FullPacket send(final Frame... frames) {
            return connection.send(frames);
          }

          @Override
          public void closeConnection(
              final TransportError error, final FrameType frameType, final String msg) {
            connection.close(error, frameType, msg);
          }
        };

    if (iter.hasNext()) {
      final InboundHandler handler = iter.next();
      handler.onReceivePacket(packet, ctx);
    }
  }

  public void send(Connection connection, Packet packet) {
    final Iterator<OutboundHandler> iter = outboundHandlers.iterator();

    PipelineContext ctx =
        new PipelineContext() {
          @Override
          public void next(final Packet newPacket) {
            if (iter.hasNext()) {
              final OutboundHandler handler = iter.next();
              handler.beforeSendPacket(newPacket, this);
            }
          }

          @Override
          public FullPacket send(final Frame... frames) {
            return connection.send(frames);
          }

          @Override
          public void closeConnection(
              final TransportError error, final FrameType frameType, final String msg) {
            connection.close(error, frameType, msg);
          }
        };

    if (iter.hasNext()) {
      final OutboundHandler handler = iter.next();
      handler.beforeSendPacket(packet, ctx);
    }
  }
}
