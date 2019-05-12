package com.protocol7.quincy.netty;

import com.protocol7.quincy.protocol.ConnectionId;
import io.netty.buffer.ByteBuf;
import io.netty.channel.DefaultAddressedEnvelope;
import java.net.InetSocketAddress;

public class QuicPacket
    extends DefaultAddressedEnvelope<
        ByteBuf, InetSocketAddress> /* TODO implements ByteBufHolder*/ {

  private final ConnectionId localConnectionId;
  private final long streamId;

  public QuicPacket(
      final ConnectionId localConnectionId,
      final long streamId,
      final ByteBuf message,
      final InetSocketAddress recipient) {
    super(message, recipient);

    this.localConnectionId = localConnectionId;
    this.streamId = streamId;
  }

  public ConnectionId getLocalConnectionId() {
    return localConnectionId;
  }

  public long getStreamId() {
    return streamId;
  }
}
