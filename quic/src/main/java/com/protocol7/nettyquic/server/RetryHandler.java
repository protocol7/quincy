package com.protocol7.nettyquic.server;

import com.protocol7.nettyquic.InboundHandler;
import com.protocol7.nettyquic.PipelineContext;
import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.protocol.packets.RetryPacket;
import com.protocol7.nettyquic.utils.Rnd;
import java.util.Optional;

public class RetryHandler implements InboundHandler {
  @Override
  public void onReceivePacket(final Packet packet, final PipelineContext ctx) {
    // TODO validate state

    if (packet instanceof InitialPacket) {
      InitialPacket initialPacket = (InitialPacket) packet;
      if (initialPacket.getToken().isPresent()) {
        // TODO validate token
      } else {
        // send retry

        byte[] retryToken = Rnd.rndBytes(34); // TODO generate a useful token

        // TODO close old connection

        ConnectionId newLocalConnectionId = ConnectionId.random();

        ctx.sendPacket(
            new RetryPacket(
                ctx.getVersion(),
                initialPacket.getSourceConnectionId(),
                Optional.of(newLocalConnectionId),
                initialPacket.getDestinationConnectionId().get(),
                retryToken));

        // don't propagate packet
        return;
      }
    }

    ctx.next(packet);
  }
}
