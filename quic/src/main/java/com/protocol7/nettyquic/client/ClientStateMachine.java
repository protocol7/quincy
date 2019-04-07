package com.protocol7.nettyquic.client;

import com.google.common.annotations.VisibleForTesting;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.*;
import com.protocol7.nettyquic.protocol.packets.*;
import com.protocol7.nettyquic.tls.ClientTlsSession;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.InitialAEAD;
import com.protocol7.nettyquic.tls.extensions.TransportParameters;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientStateMachine {

  private final Logger log = LoggerFactory.getLogger(ClientStateMachine.class);

  private State state = State.Started;
  private final ClientConnection connection;
  private final DefaultPromise<Void> handshakeFuture =
      new DefaultPromise(GlobalEventExecutor.INSTANCE); // TODO use what event executor?
  private final TransportParameters transportParameters;
  private ClientTlsSession tlsSession;

  public ClientStateMachine(
      final ClientConnection connection, TransportParameters transportParameters) {
    this.connection = connection;
    this.transportParameters = transportParameters;

    resetTlsSession(connection.getRemoteConnectionId().get());
  }

  public void resetTlsSession(ConnectionId remoteConnectionId) {
    this.tlsSession =
        new ClientTlsSession(
            InitialAEAD.create(remoteConnectionId.asBytes(), true), transportParameters);
  }

  public boolean available(EncryptionLevel encLevel) {
    return tlsSession.available(encLevel);
  }

  public Future<Void> handshake() {
    synchronized (this) {
      // send initial packet
      if (state == State.Started) {

        sendInitialPacket();
        state = State.BeforeHello;
        log.info("Client connection state initial sent");
      } else {
        throw new IllegalStateException("Can't handshake in state " + state);
      }
    }
    return handshakeFuture;
  }

  private void sendInitialPacket() {
    int len = 1200;

    final CryptoFrame clientHello = new CryptoFrame(0, tlsSession.startHandshake());
    len -= clientHello.calculateLength();

    connection.send(clientHello, new PaddingFrame(len));
  }

  public void handlePacket(Packet packet) {
    log.info("Client got {} in state {}: {}", packet.getClass().getCanonicalName(), state, packet);

    synchronized (this) { // TODO refactor to make non-synchronized
      // TODO validate connection ID
      if (state == State.BeforeHello) {
        if (packet instanceof InitialPacket) {

          connection.setRemoteConnectionId(packet.getSourceConnectionId().get(), false);

          for (final Frame frame : ((InitialPacket) packet).getPayload().getFrames()) {
            if (frame instanceof CryptoFrame) {
              final CryptoFrame cf = (CryptoFrame) frame;

              final AEAD handshakeAead = tlsSession.handleServerHello(cf.getCryptoData());
              tlsSession.setHandshakeAead(handshakeAead);
              state = State.BeforeHandshake;
            }
          }
          log.info("Client connection state ready");
        } else if (packet instanceof RetryPacket) {
          final RetryPacket retryPacket = (RetryPacket) packet;
          connection.setRemoteConnectionId(packet.getSourceConnectionId().get(), true);
          connection.resetSendPacketNumber();
          connection.setToken(retryPacket.getRetryToken());

          resetTlsSession(connection.getRemoteConnectionId().get());

          sendInitialPacket();
        } else if (packet instanceof VersionNegotiationPacket) {
          // we only support a single version, so nothing more to do
          log.debug("Incompatible versions, closing connection");
          state = State.Closing;
          connection.closeByPeer().awaitUninterruptibly(); // TODO fix, make async
          log.debug("Connection closed");
          state = State.Closed;
        } else {
          log.warn("Got packet in an unexpected state: {} - {}", state, packet);
        }
      } else if (state == State.BeforeHandshake) {
        if (packet instanceof HandshakePacket) {
          handleHandshake((HandshakePacket) packet);
        } else {
          log.warn("Got handshake packet in an unexpected state: {} - {}", state, packet);
        }

      } else if (state == State.Ready
          || state == State.Closing
          || state == State.Closed) { // TODO don't allow when closed
        if (packet instanceof FullPacket) {
          for (Frame frame : ((FullPacket) packet).getPayload().getFrames()) {
            handleFrame(frame);
          }
        }
      } else {
        log.warn("Got packet in an unexpected state {} {}", state, packet);
      }
    }
  }

  private void handleHandshake(final HandshakePacket packet) {
    for (final Frame frame : packet.getPayload().getFrames()) {
      if (frame instanceof CryptoFrame) {
        final CryptoFrame cf = (CryptoFrame) frame;

        final Optional<ClientTlsSession.HandshakeResult> result =
            tlsSession.handleHandshake(cf.getCryptoData());

        if (result.isPresent()) {
          tlsSession.setOneRttAead(result.get().getOneRttAead());

          connection.sendPacket(
              HandshakePacket.create(
                  connection.getRemoteConnectionId(),
                  connection.getLocalConnectionId(),
                  connection.nextSendPacketNumber(),
                  connection.getVersion(),
                  new CryptoFrame(0, result.get().getFin())));

          state = State.Ready;
          handshakeFuture.setSuccess(null);
        }
      }
    }
  }

  private void handleFrame(final Frame frame) {
    if (frame instanceof PingFrame) {
      // do nothing, will be acked
    } else if (frame instanceof ConnectionCloseFrame) {
      handlePeerClose();
    }
  }

  private void handlePeerClose() {
    log.debug("Peer closing connection");
    state = State.Closing;
    connection.closeByPeer().awaitUninterruptibly(); // TODO fix, make async
    log.debug("Connection closed");
    state = State.Closed;
  }

  public void closeImmediate(final ConnectionCloseFrame ccf) {
    connection.send(ccf);

    state = State.Closing;

    state = State.Closed;
  }

  public void closeImmediate() {
    closeImmediate(
        ConnectionCloseFrame.connection(
            TransportError.NO_ERROR.getValue(), 0, "Closing connection"));
  }

  @VisibleForTesting
  protected State getState() {
    return state;
  }

  public void setState(final State state) {
    this.state = state;
  }

  public AEAD getAEAD(final EncryptionLevel level) {
    return tlsSession.getAEAD(level);
  }
}
