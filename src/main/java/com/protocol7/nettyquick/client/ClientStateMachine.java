package com.protocol7.nettyquick.client;

import com.google.common.annotations.VisibleForTesting;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.ShortPacket;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.frames.PingFrame;
import com.protocol7.nettyquick.protocol.frames.PongFrame;
import com.protocol7.nettyquick.protocol.frames.RstStreamFrame;
import com.protocol7.nettyquick.protocol.frames.StreamFrame;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.streams.Stream;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientStateMachine {

  private final Logger log = LoggerFactory.getLogger(ClientStateMachine.class);

  protected enum ClientState {
    BeforeInitial,
    InitialSent,
    Ready
  }

  private ClientState state = ClientState.BeforeInitial;
  private final ClientConnection connection;
  private final DefaultPromise<Void> handshakeFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);  // TODO use what event executor?

  public ClientStateMachine(final ClientConnection connection) {
    this.connection = connection;
  }

  public Future<Void> handshake() {
    synchronized (this) {
      // send initial packet
      if (state == ClientState.BeforeInitial) {
        connection.sendPacket(InitialPacket.create(connection.getConnectionId().get()));
        state = ClientState.InitialSent;
        log.info("Client connection state inital sent");
      } else {
        throw new IllegalStateException("Can't handshake in state " + state);
      }
    }
    return handshakeFuture;
  }

  public void processPacket(Packet packet) {
    log.info("Client got {} in state {} with connection ID {}", packet.getPacketType(), state, packet.getConnectionId());

    synchronized (this) { // TODO refactor to make non-syncronized
      // TODO validate connection ID
      if (packet.getPacketType() == PacketType.Handshake) {
        if (state == ClientState.InitialSent) {
          state = ClientState.Ready;
          handshakeFuture.setSuccess(null);
          log.info("Client connection state ready");
        } else {
          log.warn("Got Handshake packet in an unexpected state: " + state);
        }
      } else if (state == ClientState.Ready) {
        for (Frame frame : packet.getPayload().getFrames()) {
          if (frame instanceof StreamFrame) {
            StreamFrame sf = (StreamFrame) frame;

            Stream stream = connection.getOrCreateStream(sf.getStreamId());
            stream.onData(sf.getOffset(), sf.getData());
          } else if (frame instanceof RstStreamFrame) {
            RstStreamFrame rsf = (RstStreamFrame) frame;
            Stream stream = connection.getOrCreateStream(rsf.getStreamId());
            stream.onReset(rsf.getApplicationErrorCode(), rsf.getOffset());
          } else if (frame instanceof PingFrame) {
            PingFrame pf = (PingFrame) frame;
            if (!pf.isEmpty()) {
              connection.sendPacket(new PongFrame(pf.getData()));
            }
          }
        }
      } else {
        log.warn("Got packet in an unexpected state");
      }
    }
  }

  @VisibleForTesting
  protected ClientState getState() {
    return state;
  }
}
