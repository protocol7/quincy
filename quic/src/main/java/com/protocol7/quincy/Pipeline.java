package com.protocol7.quincy;

import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.EncryptionLevel;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Pipeline {

  private final List<InboundHandler> inboundHandlers;
  private final List<OutboundHandler> outboundHandlers;

  public Pipeline(
      final List<InboundHandler> inboundHandlers, final List<OutboundHandler> outboundHandlers) {
    this.inboundHandlers = inboundHandlers;
    this.outboundHandlers = outboundHandlers;
  }

  public void onPacket(final Connection connection, final Packet packet) {
    final Iterator<InboundHandler> iter = inboundHandlers.iterator();

    final PipelineContext ctx =
        new PipelineContext() {
          @Override
          public void next(final Packet newPacket) {
            if (iter.hasNext()) {
              final InboundHandler handler = iter.next();
              handler.onReceivePacket(newPacket, this);
            }
          }

          @Override
          public Version getVersion() {
            return connection.getVersion();
          }

          @Override
          public InetSocketAddress getPeerAddress() {
            return connection.getPeerAddress();
          }

          @Override
          public State getState() {
            return connection.getState();
          }

          @Override
          public void setState(final State state) {
            connection.setState(state);
          }

          @Override
          public boolean isOpen() {
            return connection.isOpen();
          }

          @Override
          public Packet sendPacket(final Packet p) {
            return connection.sendPacket(p);
          }

          @Override
          public FullPacket send(final EncryptionLevel level, final Frame... frames) {
            return connection.send(level, frames);
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

  public Packet send(final Connection connection, final Packet packet) {
    final Iterator<OutboundHandler> iter = outboundHandlers.iterator();
    final AtomicReference<Packet> processedPacket = new AtomicReference<>();

    final PipelineContext ctx =
        new PipelineContext() {
          @Override
          public void next(final Packet newPacket) {
            if (iter.hasNext()) {
              final OutboundHandler handler = iter.next();
              handler.beforeSendPacket(newPacket, this);
            } else {
              processedPacket.set(newPacket);
            }
          }

          @Override
          public Version getVersion() {
            return connection.getVersion();
          }

          @Override
          public InetSocketAddress getPeerAddress() {
            return connection.getPeerAddress();
          }

          @Override
          public State getState() {
            return connection.getState();
          }

          @Override
          public void setState(final State state) {
            connection.setState(state);
          }

          @Override
          public boolean isOpen() {
            return connection.isOpen();
          }

          @Override
          public Packet sendPacket(final Packet p) {
            return connection.sendPacket(p);
          }

          @Override
          public FullPacket send(final EncryptionLevel level, final Frame... frames) {
            return connection.send(level, frames);
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

    return processedPacket.get();
  }
}
