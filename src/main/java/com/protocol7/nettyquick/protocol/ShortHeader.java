package com.protocol7.nettyquick.protocol;

import java.util.Optional;

import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

public class ShortHeader implements Header {

  public static ShortHeader parse(ByteBuf bb, LastPacketNumberProvider lastAckedProvider) {
    byte firstByte = bb.readByte();

    byte ptByte = (byte)((firstByte & 0b00011111) & 0xFF);
    boolean omitConnectionId = (firstByte & 0x40) == 0x40;
    boolean keyPhase = (firstByte & 0x20) == 0x20;

    PacketType packetType = PacketType.read(ptByte);
    Optional<ConnectionId> connId;
    if (!omitConnectionId) {
      connId = Optional.of(ConnectionId.read(12, bb)); // TODO how to determine length?
    } else {
      connId = Optional.empty();
    }
    PacketNumber lastAcked = lastAckedProvider.getLastAcked(connId.get());
    PacketNumber packetNumber;
    if (packetType == PacketType.Four_octets) {
      packetNumber = PacketNumber.read4(bb, lastAcked);
    } else if (packetType == PacketType.Two_octets) {
      packetNumber = PacketNumber.read2(bb, lastAcked);
    } else {
      packetNumber = PacketNumber.read1(bb, lastAcked);
    }
    Payload payload = Payload.parse(bb);

    return new ShortHeader(omitConnectionId,
                           keyPhase,
                           packetType,
                           connId,
                           packetNumber,
                           payload);
  }

  public static ShortHeader addFrame(ShortHeader packet, Frame frame) {
    return new ShortHeader(packet.omitConnectionId,
                           packet.keyPhase,
                           packet.packetType,
                           packet.connectionId,
                           packet.packetNumber,
                           packet.payload.addFrame(frame));
  }

  private final boolean omitConnectionId;
  private final boolean keyPhase;
  private final PacketType packetType;
  private final Optional<ConnectionId> connectionId;
  private final PacketNumber packetNumber;
  private final Payload payload;

  public ShortHeader(final boolean omitConnectionId, final boolean keyPhase, final PacketType packetType, final Optional<ConnectionId> connectionId, final PacketNumber packetNumber, final Payload payload) {
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
  public Optional<ConnectionId> getDestinationConnectionId() {
    return connectionId;
  }

  public PacketNumber getPacketNumber() {
    return packetNumber;
  }

  public Payload getPayload() {
    return payload;
  }

  public void write(ByteBuf bb) {
    byte b = packetType.getType();
    if (omitConnectionId) {
      b = (byte)(b | 0x40);
    }
    if (keyPhase) {
      b = (byte)(b | 0x20);
    }
    bb.writeByte(b);

    if (!omitConnectionId) {
      connectionId.get().write(bb); // handle omotted
    }
    if (packetType == PacketType.Four_octets) {
      packetNumber.write4(bb);
    } else if (packetType == PacketType.Two_octets) {
      packetNumber.write2(bb);
    } else if (packetType == PacketType.One_octet) {
      packetNumber.write1(bb);
    }

    payload.write(bb);
  }

  @Override
  public String toString() {
    return "ShortHeader{" +
            "packetType=" + packetType +
            ", connectionId=" + connectionId +
            ", packetNumber=" + packetNumber +
            ", payload=" + payload +
            '}';
  }
}
