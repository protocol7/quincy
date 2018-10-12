package com.protocol7.nettyquick.connection;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Header;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.packets.Packet;

public interface Connection {

  Packet sendPacket(Packet p);
  Packet sendPacket(Frame... frames);

  Optional<ConnectionId> getSourceConnectionId();
  Optional<ConnectionId> getDestinationConnectionId();

  Version getVersion();

  PacketNumber nextSendPacketNumber();
  PacketNumber lastAckedPacketNumber();

  void onPacket(Packet packet);
}
