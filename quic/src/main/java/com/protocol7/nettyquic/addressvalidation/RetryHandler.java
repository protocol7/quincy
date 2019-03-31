package com.protocol7.nettyquic.addressvalidation;

import com.protocol7.nettyquic.InboundHandler;
import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.RetryPacket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RetryHandler implements InboundHandler {

  private final RetryToken retryTokenManager;
  private final long ttlMs;

  public RetryHandler(final RetryToken retryTokenManager, final long ttl, final TimeUnit timeUnit) {
    this.retryTokenManager = retryTokenManager;
    this.ttlMs = timeUnit.toMillis(ttl);
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    // TODO validate state

    if (packet instanceof InitialPacket) {
      final InitialPacket initialPacket = (InitialPacket) packet;

      if (initialPacket.getToken().isPresent()) {
        if (retryTokenManager.validate(
            initialPacket.getToken().get(), ctx.getPeerAddress().getAddress(), now())) {
          // good to go
        } else {
          // send retry
          sendRetry(ctx, initialPacket);
        }
      } else {
        // send retry
        // TODO close old connection

        sendRetry(ctx, initialPacket);

        // don't propagate packet
        return;
      }
    }

    ctx.next(packet);
  }

  private void sendRetry(final PipelineContext ctx, final InitialPacket initialPacket) {
    byte[] retryToken = retryTokenManager.create(ctx.getPeerAddress().getAddress(), now() + ttlMs);

    final ConnectionId newLocalConnectionId = ConnectionId.random();

    ctx.sendPacket(
        new RetryPacket(
            ctx.getVersion(),
            initialPacket.getSourceConnectionId(),
            Optional.of(newLocalConnectionId),
            initialPacket.getDestinationConnectionId().get(),
            retryToken));
  }

  private long now() {
    return System.currentTimeMillis();
  }
}
