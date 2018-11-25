package com.protocol7.nettyquick.connection;

import com.protocol7.nettyquick.EncryptionLevel;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.protocol.packets.FullPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;
import com.protocol7.nettyquick.tls.aead.AEAD;
import java.util.Optional;

public interface Connection {

  Packet sendPacket(Packet p);

  FullPacket sendPacket(Frame... frames);

  Optional<ConnectionId> getSourceConnectionId();

  Optional<ConnectionId> getDestinationConnectionId();

  Version getVersion();

  PacketNumber nextSendPacketNumber();

  PacketNumber lastAckedPacketNumber();

  void onPacket(Packet packet);

  AEAD getAEAD(EncryptionLevel level);
}
