package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.packets.Packet;

public interface InternalConnection extends Connection {

  void onPacket(Packet packet);

  PacketNumber nextSendPacketNumber();

  void setState(State state);
}
