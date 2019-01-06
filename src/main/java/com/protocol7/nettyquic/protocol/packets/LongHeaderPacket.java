package com.protocol7.nettyquic.protocol.packets;

import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.tls.aead.AEAD;
import io.netty.buffer.ByteBuf;

import java.util.Objects;
import java.util.Optional;

public abstract class LongHeaderPacket implements FullPacket {

  private final PacketType packetType;
  private final Optional<ConnectionId> destinationConnectionId;
  private final Optional<ConnectionId> sourceConnectionId;
  private final Version version;
  private final PacketNumber packetNumber;
  private final Payload payload;

  public LongHeaderPacket(
      PacketType packetType,
      Optional<ConnectionId> destinationConnectionId,
      Optional<ConnectionId> sourceConnectionId,
      Version version,
      PacketNumber packetNumber,
      Payload payload) {
    this.packetType = packetType;
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;
    this.version = version;
    this.packetNumber = packetNumber;
    this.payload = payload;
  }

  @Override
  public PacketType getType() {
    return PacketType.Initial;
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return destinationConnectionId;
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return sourceConnectionId;
  }

  public Version getVersion() {
    return version;
  }

  @Override
  public PacketNumber getPacketNumber() {
    return packetNumber;
  }

  @Override
  public Payload getPayload() {
    return payload;
  }

  protected void writePrefix(ByteBuf bb) {
    int b = (PACKET_TYPE_MASK | packetType.getType() << 4) & 0xFF;
    b = b | 0x40; // fixed

    int pnLen = packetNumber.getLength();
    b = (byte) (b | (pnLen - 1)); // pn length
    bb.writeByte(b);

    version.write(bb);

    int cil = ConnectionId.joinLenghts(destinationConnectionId, sourceConnectionId);
    bb.writeByte(cil);

    if (destinationConnectionId.isPresent()) {
      destinationConnectionId.get().write(bb);
    }
    if (sourceConnectionId.isPresent()) {
      sourceConnectionId.get().write(bb);
    }
  }

  protected void writeSuffix(ByteBuf bb, AEAD aead) {
    byte[] pn = packetNumber.write(packetNumber.getLength());

    Varint.write(payload.calculateLength() + pn.length, bb);

    bb.writeBytes(pn);

    byte[] aad = new byte[bb.writerIndex()];
    bb.markReaderIndex();
    bb.readBytes(aad);
    bb.resetReaderIndex();

    payload.write(bb, aead, packetNumber, aad);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LongHeaderPacket that = (LongHeaderPacket) o;
    return packetType == that.packetType
        && Objects.equals(destinationConnectionId, that.destinationConnectionId)
        && Objects.equals(sourceConnectionId, that.sourceConnectionId)
        && version == that.version
        && Objects.equals(packetNumber, that.packetNumber)
        && Objects.equals(payload, that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        packetType, destinationConnectionId, sourceConnectionId, version, packetNumber, payload);
  }
}
