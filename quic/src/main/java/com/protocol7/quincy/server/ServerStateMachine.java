package com.protocol7.quincy.server;

import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.EncryptionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStateMachine {

  private final Logger log = LoggerFactory.getLogger(ServerStateMachine.class);

  public State getState() {
    return state;
  }

  private State state = State.Started;
  private final ServerConnection connection;

  public ServerStateMachine(final ServerConnection connection) {
    this.connection = connection;
  }

  public synchronized void processPacket(final Packet packet) {
    // TODO check version
    if (state == State.Started) {
      if (packet instanceof InitialPacket) {
        final InitialPacket initialPacket = (InitialPacket) packet;

        if (initialPacket.getToken().isPresent()) {
          connection.setRemoteConnectionId(packet.getSourceConnectionId());
        }
      }
    }
  }

  public void closeImmediate(final ConnectionCloseFrame ccf) {
    connection.send(EncryptionLevel.OneRtt, ccf);

    state = State.Closing;

    state = State.Closed;
  }

  public void closeImmediate() {
    closeImmediate(
        new ConnectionCloseFrame(
            TransportError.NO_ERROR.getValue(), FrameType.PADDING, "Closing connection"));
  }

  public void setState(final State state) {
    this.state = state;
  }
}
