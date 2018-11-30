package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.tls.aead.AEAD;
import io.netty.buffer.ByteBuf;
import java.util.Optional;

public interface Header {

  // TODO how does this work when omitting the conn id?
  Optional<ConnectionId> getDestinationConnectionId();

  PacketNumber getPacketNumber();

  void write(ByteBuf bb, AEAD aead);
}
