package com.protocol7.nettyquick.connection;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.tls.AEAD;

public interface Connection {

  Packet sendPacket(Packet p);
  FullPacket sendPacket(Frame... frames);

  Optional<ConnectionId> getSourceConnectionId();
  Optional<ConnectionId> getDestinationConnectionId();

  Version getVersion();

  PacketNumber nextSendPacketNumber();
  PacketNumber lastAckedPacketNumber();

  void onPacket(Packet packet);

  AEAD getAEAD(PacketType packetType);
}
