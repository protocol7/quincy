package com.protocol7.nettyquick.streams;

import static com.protocol7.nettyquick.streams.ReceiveStateMachine.ReceiveStreamState.*;

public class ReceiveStateMachine {

  public enum ReceiveStreamState {
    Recv,
    SizeKnown,
    DataRecvd,
    ResetRecvd,
    DataRead,
    ResetRead
  }

  private ReceiveStreamState state = Recv;

  public void onStream(boolean fin) {
    if (state == Recv) {
      if (fin) {
        state = SizeKnown;
      }
    } else {
      throw new IllegalStateException();
    }
  }

  public void onReset() {
    if (state == Recv || state == SizeKnown || state == DataRecvd) {
      state = ResetRecvd;
    } else {
      throw new IllegalStateException();
    }
  }

  public void onAllData() {
    if (state == DataRecvd) {
      state = DataRead;
    } else {
      throw new IllegalStateException();
    }
  }

  public void onAppReadReset() {
    if (state == ResetRecvd) {
      state = ResetRead;
    } else {
      throw new IllegalStateException();
    }
  }

  public boolean canReceive() {
    return state == Recv;
  }

  public boolean canReset() {
    return state == Recv || state == SizeKnown || state == DataRecvd;
  }
}
