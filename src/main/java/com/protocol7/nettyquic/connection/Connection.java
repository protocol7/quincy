package com.protocol7.nettyquic.connection;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;
import com.protocol7.nettyquic.protocol.TransportParameters;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.protocol.packets.FullPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.aead.AEAD;
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

  Optional<TransportParameters> getPeerTransportParameters();
}
