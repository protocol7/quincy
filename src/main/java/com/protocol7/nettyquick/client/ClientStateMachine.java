package com.protocol7.nettyquick.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.*;
import com.protocol7.nettyquick.protocol.packets.*;
import com.protocol7.nettyquick.streams.Stream;
import com.protocol7.nettyquick.tls.ClientTlsSession;
import com.protocol7.nettyquick.tls.aead.AEAD;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ClientStateMachine {

  private final Logger log = LoggerFactory.getLogger(ClientStateMachine.class);

  protected enum ClientState {
    BeforeInitial,
    WaitingForServerHello,
    WaitingForHandshake,
    Ready;
  }

  private ClientState state = ClientState.BeforeInitial;
  private final ClientConnection connection;
  private final DefaultPromise<Void> handshakeFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);  // TODO use what event executor?
  private final ClientTlsSession tlsEngine = new ClientTlsSession();


  public ClientStateMachine(final ClientConnection connection) {
    this.connection = connection;
  }

  public Future<Void> handshake() {
    synchronized (this) {
      // send initial packet
      if (state == ClientState.BeforeInitial) {

        sendInitialPacket(Optional.empty());
        state = ClientState.WaitingForServerHello;
        log.info("Client connection state initial sent");
      } else {
        throw new IllegalStateException("Can't handshake in state " + state);
      }
    }
    return handshakeFuture;
  }

  private void sendInitialPacket(Optional<byte[]> token) {
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
            connection.nextSendPacketNumber(),
            Version.CURRENT,
            token,
            frames));
  }

  public void handlePacket(Packet packet) {
    log.info("Client got {} in state {} with connection ID {}", packet.getClass().getName(), state, packet.getDestinationConnectionId());

    synchronized (this) { // TODO refactor to make non-synchronized
      // TODO validate connection ID
      if (state == ClientState.WaitingForServerHello) {
        if (packet instanceof InitialPacket) {

          connection.setDestinationConnectionId(packet.getSourceConnectionId().get());

          for (Frame frame : ((InitialPacket)packet).getPayload().getFrames()) {
            if (frame instanceof CryptoFrame) {
              CryptoFrame cf = (CryptoFrame) frame;

              AEAD handshakeAead = tlsEngine.handleServerHello(cf.getCryptoData());
              connection.setHandshakeAead(handshakeAead);
              state = ClientState.WaitingForHandshake;
            }
          }

          log.info("Client connection state ready");
        } else if (packet instanceof RetryPacket) {
          RetryPacket retryPacket = (RetryPacket) packet;
          connection.setDestinationConnectionId(ConnectionId.random());
          connection.setSourceConnectionId(packet.getSourceConnectionId());
          connection.resetSendPacketNumber();

          tlsEngine.reset();

          sendInitialPacket(Optional.of(retryPacket.getRetryToken()));
        } else {
          log.warn("Got packet in an unexpected state: {} - {}", state, packet);
        }
      } else if (state == ClientState.WaitingForHandshake) {
        if (packet instanceof HandshakePacket) {
          for (Frame frame : ((HandshakePacket)packet).getPayload().getFrames()) {
            if (frame instanceof CryptoFrame) {
              CryptoFrame cf = (CryptoFrame) frame;

              Optional<ClientTlsSession.HandshakeResult> result = tlsEngine.handleHandshake(cf.getCryptoData());

              if (result.isPresent()) {
                connection.setOneRttAead(result.get().getOneRttAead());

                connection.sendPacket(HandshakePacket.create(
                        connection.getDestinationConnectionId(),
                        connection.getSourceConnectionId(),
                        connection.nextSendPacketNumber(),
                        Version.TLS_DEV,
                        new CryptoFrame(0, result.get().getFin())));

                state = ClientState.Ready;
                handshakeFuture.setSuccess(null);
              }
            }
          }
        } else {
          log.warn("Got handshake packet in an unexpected state: {} - {}", state, packet);
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
