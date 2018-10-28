package com.protocol7.nettyquick.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Varint;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.HandshakePacket;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.tls.TlsEngine;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
  private final TlsEngine tlsEngine = new TlsEngine(true);

  public ClientStateMachine(final ClientConnection connection) {
    this.connection = connection;
  }

  public Future<Void> handshake() {
    synchronized (this) {
      // send initial packet
      if (state == ClientState.BeforeInitial) {

        List<Frame> frames = Lists.newArrayList();

        int len = 1200;

        CryptoFrame clientHello = new CryptoFrame(0, tlsEngine.start());
        len -= clientHello.calculateLength();
        frames.add(clientHello);
        for (int i = len; i>0; i--) {
          frames.add(PaddingFrame.INSTANCE);
        }

        connection.sendPacket(InitialPacket.create(
                connection.getDestinationConnectionId(),
                connection.getSourceConnectionId(),
                PacketNumber.MIN,
                Version.DRAFT_15,
                Optional.empty(),
                frames));
        state = ClientState.InitialSent;
        log.info("Client connection state initial sent");
      } else {
        throw new IllegalStateException("Can't handshake in state " + state);
      }
    }
    return handshakeFuture;
  }

  public void processPacket(Packet packet) {
    log.info("Client got {} in state {} with connection ID {}", packet.getClass().getName(), state, packet.getDestinationConnectionId());

    synchronized (this) { // TODO refactor to make non-synchronized
      // TODO validate connection ID
      if (packet instanceof InitialPacket) {
        if (state == ClientState.InitialSent) {
          state = ClientState.Ready;
          connection.setDestinationConnectionId(packet.getSourceConnectionId().get());
          handshakeFuture.setSuccess(null);
          log.info("Client connection state ready");
        } else {
          log.warn("Got Handshake packet in an unexpected state: " + state);
        }
      } else if (state == ClientState.Ready) {
        for (Frame frame : ((FullPacket)packet).getPayload().getFrames()) {
          if (frame instanceof StreamFrame) {
            StreamFrame sf = (StreamFrame) frame;

            Stream stream = connection.getOrCreateStream(sf.getStreamId());
            stream.onData(sf.getOffset(), sf.isFin(), sf.getData());
          } else if (frame instanceof RstStreamFrame) {
            RstStreamFrame rsf = (RstStreamFrame) frame;
            Stream stream = connection.getOrCreateStream(rsf.getStreamId());
            stream.onReset(rsf.getApplicationErrorCode(), rsf.getOffset());
          } else if (frame instanceof PingFrame) {
            PingFrame pf = (PingFrame) frame;
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
