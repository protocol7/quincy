package com.protocol7.nettyquic.client;

import com.google.common.annotations.VisibleForTesting;
import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.*;
import com.protocol7.nettyquic.protocol.packets.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientStateMachine {

  private final Logger log = LoggerFactory.getLogger(ClientStateMachine.class);

  private State state = State.Started;
  private final ClientConnection connection;

  public ClientStateMachine(final ClientConnection connection) {
    this.connection = connection;
  }

  public void handlePacket(Packet packet) {
    log.info("Client got {} in state {}: {}", packet.getClass().getCanonicalName(), state, packet);

    synchronized (this) { // TODO refactor to make non-synchronized
      // TODO validate connection ID
      if (state == State.BeforeHello) {
        if (packet instanceof InitialPacket) {
          connection.setRemoteConnectionId(packet.getSourceConnectionId().get(), false);
        } else if (packet instanceof RetryPacket) {
          final RetryPacket retryPacket = (RetryPacket) packet;
          connection.setRemoteConnectionId(packet.getSourceConnectionId().get(), true);
          connection.resetSendPacketNumber();
          connection.setToken(retryPacket.getRetryToken());
        } else if (packet instanceof VersionNegotiationPacket) {
          // we only support a single version, so nothing more to do
          log.debug("Incompatible versions, closing connection");
          state = State.Closing;
          connection.closeByPeer().awaitUninterruptibly(); // TODO fix, make async
          log.debug("Connection closed");
          state = State.Closed;
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
}
