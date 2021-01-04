package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.tls.EncryptionLevel;

public class ServerStateMachine extends StateMachine {

  private final ServerConnection connection;

  public ServerStateMachine(final ServerConnection connection) {
    this.connection = connection;
  }

  public synchronized void handlePacket(final Packet packet) {
    // TODO check version
    if (getState() == State.Started) {
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

    setState(State.Closing);

    setState(State.Closed);
  }

  public void closeImmediate() {
    closeImmediate(
        new ConnectionCloseFrame(
            TransportError.NO_ERROR.getValue(), FrameType.PADDING, "Closing connection"));
  }
}
