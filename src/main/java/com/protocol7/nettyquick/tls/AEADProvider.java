package com.protocol7.nettyquick.tls;

import com.protocol7.nettyquick.protocol.ConnectionId;

public interface AEADProvider {

    AEAD forConnection(ConnectionId connId);


}
