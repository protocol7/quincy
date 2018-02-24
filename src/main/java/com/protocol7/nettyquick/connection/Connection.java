package com.protocol7.nettyquick.connection;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Packet;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.connection.Sender;
import com.protocol7.nettyquick.protocol.Version;

public interface Connection {

  void sendPacket(Packet p);

  Optional<ConnectionId> getConnectionId();

  Version getVersion();

  PacketNumber nextPacketNumber();

  void onPacket(Packet packet);
}
