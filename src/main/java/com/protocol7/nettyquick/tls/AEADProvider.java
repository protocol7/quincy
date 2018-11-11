package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.PacketType;

import java.util.Optional;

public interface AEADProvider {

    AEAD forConnection(Optional<ConnectionId> connId, PacketType packetType);


}
