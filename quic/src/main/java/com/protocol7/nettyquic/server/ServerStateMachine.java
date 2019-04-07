package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.connection.State;
import com.protocol7.nettyquic.protocol.TransportError;
import com.protocol7.nettyquic.protocol.frames.ConnectionCloseFrame;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
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

  public synchronized void processPacket(Packet packet) {
    // TODO check version
    if (state == State.Started) {
      if (packet instanceof InitialPacket) {
        InitialPacket initialPacket = (InitialPacket) packet;

        if (initialPacket.getToken().isPresent()) {
          if (packet.getSourceConnectionId().isPresent()) {
            connection.setRemoteConnectionId(packet.getSourceConnectionId().get());
          }
        }
      }
    } else if (state == State.Ready) {
      handleFrames((FullPacket) packet);
    }
  }

  private void handleFrames(FullPacket packet) {
    for (Frame frame : packet.getPayload().getFrames()) {
      if (frame instanceof ConnectionCloseFrame) {
        handlePeerClose();
      }
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

  public void setState(final State state) {
    this.state = state;
  }
}
