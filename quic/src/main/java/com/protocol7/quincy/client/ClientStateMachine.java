package com.protocol7.quincy.client;

import com.protocol7.quincy.connection.ClientConnection;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.protocol.packets.VersionNegotiationPacket;
import com.protocol7.quincy.tls.EncryptionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientStateMachine {

  private final Logger log = LoggerFactory.getLogger(ClientStateMachine.class);

  private State state = State.Started;
  private final ClientConnection connection;

  public ClientStateMachine(final ClientConnection connection) {
    this.connection = connection;
  }

  public void handlePacket(final Packet packet) {
    log.info("Client got {} in state {}: {}", packet.getClass().getCanonicalName(), state, packet);

    synchronized (this) { // TODO refactor to make non-synchronized
      // TODO validate connection ID
      if (state == State.BeforeHello) {
        if (packet instanceof InitialPacket) {
          connection.setRemoteConnectionId(packet.getSourceConnectionId(), false);
        } else if (packet instanceof RetryPacket) {
          final RetryPacket retryPacket = (RetryPacket) packet;
          connection.setRemoteConnectionId(packet.getSourceConnectionId(), true);
          connection.resetSendPacketNumber();
          connection.setToken(retryPacket.getRetryToken());
        } else if (packet instanceof VersionNegotiationPacket) {
          // we only support a single version, so nothing more to do
          log.debug("Incompatible versions, closing connection");
          state = State.Closing;
          connection.closeByPeer();
          log.debug("Connection closed");
          state = State.Closed;
        }
      }
    }
  }

  public void closeImmediate(final ConnectionCloseFrame ccf) {
    // TODO verify level
    connection.send(EncryptionLevel.OneRtt, ccf);

    state = State.Closing;

    state = State.Closed;
  }

  public void closeImmediate() {
    closeImmediate(
        new ConnectionCloseFrame(
            TransportError.NO_ERROR.getValue(), FrameType.PADDING, "Closing connection"));
  }

  public State getState() {
    return state;
  }

  public void setState(final State state) {
    this.state = state;
  }
}
