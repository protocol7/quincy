package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.tls.aead.AEADProvider;
import java.util.Optional;

public interface HalfParsedPacket<P extends Packet> {

  PacketType getType();

  Optional<Version> getVersion();

  ConnectionId getDestinationConnectionId();

  Optional<ConnectionId> getSourceConnectionId();

  Optional<byte[]> getRetryToken();

  P complete(AEADProvider aeadProvider);
}
