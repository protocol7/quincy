package com.protocol7.quincy.streams;

import static com.protocol7.quincy.streams.SendStateMachine.SendStreamState.*;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SendStateMachine {

  public SendStreamState getState() {
    return state;
  }

  public enum SendStreamState {
    Open,
    Send,
    DataSent,
    ResetSent,
    DataRecvd,
    ResetRecvd
  }

  private SendStreamState state = Open;
  private final Set<Long> outstandingStreamPackets =
      Collections.newSetFromMap(new ConcurrentHashMap<>());
  private Optional<Long> outstandingResetPacket = Optional.empty();

  public void onStream(final long pn, final boolean fin) {
    if (state == Open || state == Send) {
      if (fin) {
        state = DataSent;
      } else {
        state = Send;
      }
    } else {
      throw new IllegalStateException();
    }
  }

  public void onReset(final long pn) {
    if (state == Open || state == Send || state == DataSent) {
      state = ResetSent;
      outstandingResetPacket = Optional.of(pn);
    } else {
      throw new IllegalStateException();
    }
  }

  public void onAck(final long pn) {
    outstandingStreamPackets.remove(pn);

    if (state == DataSent && outstandingStreamPackets.isEmpty()) {
      state = DataRecvd;
    } else if (state == ResetSent) {
      if (outstandingResetPacket.isPresent() && outstandingResetPacket.get().equals(pn)) {
        state = ResetRecvd;
      } else {
        throw new IllegalStateException();
      }
    }
  }

  public boolean canSend() {
    return state == Open || state == Send;
  }

  public boolean canReset() {
    return state == Open || state == Send || state == DataSent;
  }
}
