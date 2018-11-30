package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.tls.aead.AEADProvider;

import java.util.Optional;

public interface HalfParsedPacket<P extends Packet> {

    Optional<Version> getVersion();

    Optional<ConnectionId> getConnectionId();

    P complete(AEADProvider aeadProvider);
}
