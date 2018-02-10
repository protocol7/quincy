package com.protocol7.nettyquick.protocol;

import java.util.Optional;

public class ShortHeader implements Packet {

  private final boolean omitConnectionId;
  private final boolean keyPhase;
  private final ShortPacketType packetType;
  private final Optional<ConnectionId> connectionId;
  private final PacketNumber packetNumber;
  private final Payload payload;

  public ShortHeader(final boolean omitConnectionId, final boolean keyPhase, final ShortPacketType packetType, final Optional<ConnectionId> connectionId, final PacketNumber packetNumber, final Payload payload) {
    this.omitConnectionId = omitConnectionId;
    this.keyPhase = keyPhase;
    this.packetType = packetType;
    this.connectionId = connectionId;
    this.packetNumber = packetNumber;
    this.payload = payload;
  }
}
