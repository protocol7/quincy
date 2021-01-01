package com.protocol7.quincy.addressvalidation;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import java.util.concurrent.TimeUnit;

public class ServerRetryHandler implements InboundHandler {

  private final RetryToken retryTokenManager;
  private final long ttlMs;

  public ServerRetryHandler(
      final RetryToken retryTokenManager, final long ttl, final TimeUnit timeUnit) {
    this.retryTokenManager = requireNonNull(retryTokenManager);
    this.ttlMs = timeUnit.toMillis(ttl);
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    requireNonNull(packet);
    requireNonNull(ctx);

    if (ctx.getState() == State.Started) {
      if (packet instanceof InitialPacket) {
        final InitialPacket initialPacket = (InitialPacket) packet;

        if (initialPacket.getToken().isPresent()) {
          if (retryTokenManager.validate(
              initialPacket.getToken().get(), ctx.getPeerAddress().getAddress(), now())) {
            // good to go
          } else {
            // send retry
            sendRetry(ctx, initialPacket);

            return;
          }
        } else {
          // send retry
          // TODO close old connection

          sendRetry(ctx, initialPacket);

          // don't propagate packet
          return;
        }
      }
    }

    ctx.next(packet);
  }

  private void sendRetry(final PipelineContext ctx, final InitialPacket initialPacket) {
    final byte[] retryToken =
        retryTokenManager.create(ctx.getPeerAddress().getAddress(), now() + ttlMs);

    final ConnectionId newLocalConnectionId = ConnectionId.random();

    ctx.sendPacket(
        RetryPacket.createOutgoing(
            ctx.getVersion(),
            initialPacket.getSourceConnectionId(),
            newLocalConnectionId,
            initialPacket.getDestinationConnectionId(),
            retryToken));
  }

  private long now() {
    return System.currentTimeMillis();
  }
}
