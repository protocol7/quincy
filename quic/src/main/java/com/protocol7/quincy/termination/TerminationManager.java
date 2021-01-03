package com.protocol7.quincy.termination;

import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.Connection;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.TransportError;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.FrameType;
import com.protocol7.quincy.protocol.packets.FullPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TerminationManager implements InboundHandler {

  private final AtomicReference<Timeout> timeout = new AtomicReference<>(null);
  private final Connection connection;
  private final Timer timer;
  private final long idleTimeoutMs;
  private final TimerTask idleTask;

  public TerminationManager(
      final Connection connection,
      final Timer timer,
      final long idleTimeout,
      final TimeUnit idleUnit) {
    this.connection = connection;
    this.timer = timer;
    this.idleTimeoutMs = idleUnit.toMillis(idleTimeout);
    this.idleTask =
        timeout -> connection.close(TransportError.NO_ERROR, FrameType.PADDING, "Timeout");
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    if (packet instanceof FullPacket) {
      final FullPacket fp = (FullPacket) packet;

      for (final Frame frame : fp.getPayload().getFrames()) {
        if (frame instanceof ConnectionCloseFrame) {
          ctx.setState(State.Closing);
          connection.closeByPeer();
          ctx.setState(State.Closed);
        }
      }
    }

    // reset idle timer on any packet
    resetIdleTimer();

    ctx.next(packet);
  }

  private void resetIdleTimer() {
    timeout.updateAndGet(
        timeout -> {
          if (timeout != null) {
            timeout.cancel();
          }
          return timer.newTimeout(idleTask, idleTimeoutMs, TimeUnit.MILLISECONDS);
        });
  }
}
