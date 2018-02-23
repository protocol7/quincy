package com.protocol7.nettyquick;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;

public interface Connection {

  void sendPacket(Packet p);

  Optional<ConnectionId> getConnectionId();

  PacketNumber nextPacketNumber();
}
