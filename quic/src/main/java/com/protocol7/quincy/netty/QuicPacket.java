package com.protocol7.quincy.netty;

import com.protocol7.quincy.protocol.ConnectionId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.DefaultAddressedEnvelope;
import java.net.InetSocketAddress;

public class QuicPacket
    extends DefaultAddressedEnvelope<
        ByteBuf, InetSocketAddress> /* TODO implements ByteBufHolder*/ {

  public static QuicPacket of(
      final ConnectionId localConnectionId,
      final long streamId,
      final byte[] message,
      final InetSocketAddress recipient) {
    final ByteBuf bb = Unpooled.wrappedBuffer(message);
    return new QuicPacket(localConnectionId, streamId, bb, recipient);
  }

  private final ConnectionId localConnectionId;
  private final long streamId;

  private QuicPacket(
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
