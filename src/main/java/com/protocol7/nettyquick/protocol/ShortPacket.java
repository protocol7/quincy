package com.protocol7.nettyquick.protocol;

import java.util.Optional;

public class ShortPacket implements Packet {

  private final boolean omitConnectionId;
  private final boolean keyPhase;
  private final PacketType packetType;
  private final Optional<ConnectionId> connectionId;
  private final PacketNumber packetNumber;
  private final Payload payload;

  public ShortPacket(final boolean omitConnectionId, final boolean keyPhase, final PacketType packetType, final Optional<ConnectionId> connectionId, final PacketNumber packetNumber, final Payload payload) {
    this.omitConnectionId = omitConnectionId;
    this.keyPhase = keyPhase;
    this.packetType = packetType;
    this.connectionId = connectionId;
    this.packetNumber = packetNumber;
    this.payload = payload;
  }

  public boolean isOmitConnectionId() {
    return omitConnectionId;
  }

  public boolean isKeyPhase() {
    return keyPhase;
  }

  public PacketType getPacketType() {
    return packetType;
  }

  @Override
  public ConnectionId getConnectionId() {
    return connectionId.get(); // TODO fix for when connId is omitted
  }

  public PacketNumber getPacketNumber() {
    return packetNumber;
  }

  public Payload getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return "ShortPacket{" +
            "packetType=" + packetType +
            ", connectionId=" + connectionId +
            ", packetNumber=" + packetNumber +
            ", payload=" + payload +
            '}';
  }
}
