package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.LongPacket;
import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.PacketNumber;
import com.protocol7.nettyquick.protocol.Payload;
import com.protocol7.nettyquick.protocol.Version;

public class HandshakePacket extends LongPacket {

  public static HandshakePacket create(ConnectionId connectionId, PacketNumber packetNumber, Version version) { // TODO take crypto params
    Payload payload = new Payload(Lists.newArrayList()); // TODO create stream and ack frames
    return new HandshakePacket(connectionId, version, packetNumber, payload);
  }

  private HandshakePacket(final ConnectionId connectionId, final Version version, final PacketNumber packetNumber, final Payload payload) {
    super(PacketType.Handshake, connectionId, version, packetNumber, payload);
  }
}
