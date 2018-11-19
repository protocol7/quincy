package com.protocol7.nettyquick.tls.aead;

import com.protocol7.nettyquick.EncryptionLevel;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.packets.Packet;

import java.util.Optional;

public interface AEADProvider {

    AEAD forConnection(Optional<ConnectionId> connId, EncryptionLevel level);


}
