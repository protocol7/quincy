package com.protocol7.nettyquick.it;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.PacketNumber;

public class QuicGoPacket {

  public final boolean inbound;
  public final boolean longHeader;

  public final String type;

  public final ConnectionId destinationConnectionId;
  public final ConnectionId sourceConnectionId;

  public final PacketNumber packetNumber;

  public QuicGoPacket(
      final boolean inbound,
      final boolean longHeader,
      final String type,
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final PacketNumber packetNumber) {
    this.inbound = inbound;
    this.longHeader = longHeader;
    this.type = type;
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;
    this.packetNumber = packetNumber;
  }

  @Override
  public String toString() {
    return "QuicGoPacket{"
        + "inbound="
        + inbound
        + ", longHeader="
        + longHeader
        + ", type='"
        + type
        + '\''
        + ", destinationConnectionId="
        + destinationConnectionId
        + ", sourceConnectionId="
        + sourceConnectionId
        + ", packetNumber="
        + packetNumber
        + '}';
  }
}
