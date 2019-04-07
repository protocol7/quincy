package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.packets.Packet;

public interface InternalConnection extends Connection {

  void onPacket(Packet packet);

  void setState(State state);
}
