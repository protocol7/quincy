package com.protocol7.quincy.connection;

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

public class ClientStateMachine extends StateMachine {

  private final Logger log = LoggerFactory.getLogger(ClientStateMachine.class);

  public void handlePacket(final Connection connection, final Packet packet) {
    log.info(
        "Client got {} in state {}: {}", packet.getClass().getCanonicalName(), getState(), packet);

    synchronized (this) { // TODO refactor to make non-synchronized
      // TODO validate connection ID
      if (getState() == State.BeforeHello) {
        if (packet instanceof InitialPacket) {
          connection.setRemoteConnectionId(packet.getSourceConnectionId());
        } else if (packet instanceof RetryPacket) {
          final RetryPacket retryPacket = (RetryPacket) packet;
          connection.reset(packet.getSourceConnectionId());

          connection.setToken(retryPacket.getRetryToken());
        } else if (packet instanceof VersionNegotiationPacket) {
          // we only support a single version, so nothing more to do
          log.debug("Incompatible versions, closing connection");
          setState(State.Closing);
          connection.closeByPeer();
          log.debug("Connection closed");
          setState(State.Closed);
        }
      }
    }
  }

  public void closeImmediate(final Connection connection, final ConnectionCloseFrame ccf) {
    // TODO verify level
    if (getState() != State.Closing && getState() != State.Closed) {
      connection.send(EncryptionLevel.OneRtt, ccf);

      setState(State.Closing);

      setState(State.Closed);
    }
  }

  public void closeImmediate(final Connection connection) {
    closeImmediate(
        connection,
        new ConnectionCloseFrame(
            TransportError.NO_ERROR.getValue(), FrameType.PADDING, "Closing connection"));
  }
}
