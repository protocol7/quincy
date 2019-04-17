package com.protocol7.quincy.connection;

import com.protocol7.quincy.protocol.packets.Packet;

public interface InternalConnection extends Connection {

  void onPacket(Packet packet);

  void setState(State state);
}
