package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.packets.Packet;

public abstract class StateMachine {
  private State state = State.Started;

  abstract void handlePacket(final Connection connection, final Packet packet);

  abstract void closeImmediate(final Connection connection, final ConnectionCloseFrame ccf);

  abstract void closeImmediate(final Connection connection);

  public State getState() {
    return state;
  }

  public void setState(final State state) {
    this.state = state;
  }
}
