package com.protocol7.quincy.addressvalidation;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.InboundHandler;
import com.protocol7.quincy.PipelineContext;
import com.protocol7.quincy.connection.State;
import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.packets.InitialPacket;
import com.protocol7.quincy.protocol.packets.Packet;
import com.protocol7.quincy.protocol.packets.RetryPacket;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;

public class ServerRetryHandler implements InboundHandler {

  private final Optional<QuicTokenHandler> tokenHandler;

  public ServerRetryHandler(final Optional<QuicTokenHandler> tokenHandler) {
    this.tokenHandler = requireNonNull(tokenHandler);
  }

  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    requireNonNull(packet);
    requireNonNull(ctx);

    if (tokenHandler.isPresent()) {

      if (ctx.getState() == State.Started) {
        if (packet instanceof InitialPacket) {
          final InitialPacket initialPacket = (InitialPacket) packet;

          if (initialPacket.getToken().isPresent()) {
            final ByteBuf token = Unpooled.wrappedBuffer(initialPacket.getToken().get());
            try {
              if (tokenHandler.get().validateToken(token, ctx.getPeerAddress()).isPresent()) {
                // good to go
              } else {
                // send retry
                sendRetry(ctx, initialPacket);

                return;
              }
            } finally {
              token.release();
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
    }
    ctx.next(packet);
  }

  private void sendRetry(final PipelineContext ctx, final InitialPacket initialPacket) {
    final ByteBuf tokenBB = Unpooled.buffer();
    tokenHandler
        .get()
        .writeToken(tokenBB, initialPacket.getSourceConnectionId(), ctx.getPeerAddress());

    final byte[] retryToken = Bytes.drainToArray(tokenBB);

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
