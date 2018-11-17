package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.AEADProvider;
import com.protocol7.nettyquick.utils.Opt;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public class LongHeader implements Header {

  public static final int PACKET_TYPE_MASK = 0b10000000;

  public static LongHeader parse(ByteBuf bb, boolean pnVarint, AEADProvider aeadProvider) {
    bb.markReaderIndex();

    byte firstByte = bb.readByte();
    byte ptByte = (byte) ((firstByte & (~PACKET_TYPE_MASK)) & 0xFF);
    PacketType packetType = PacketType.read(ptByte);

    Version version = Version.read(bb);

    int cil = bb.readByte() & 0xFF;
    int dcil = ConnectionId.firstLength(cil);
    int scil = ConnectionId.lastLength(cil);

    Optional<ConnectionId> destConnId = ConnectionId.readOptional(dcil, bb);
    Optional<ConnectionId> srcConnId = ConnectionId.readOptional(scil, bb);

    int length = (int) Varint.read(bb).getValue();
    PacketNumber packetNumber = PacketNumber.parseVarint(bb);

    int payloadLength = length; // TODO pn length

    byte[] aad = new byte[bb.readerIndex()];
    bb.resetReaderIndex();
    bb.readBytes(aad);

    AEAD aead = aeadProvider.forConnection(destConnId, packetType);

    Payload payload = Payload.parse(bb, payloadLength, aead, packetNumber, aad);

    return new LongHeader(packetType,
                          destConnId,
                          srcConnId,
                          version,
                          packetNumber,
                          payload);
  }

  public static LongHeader addFrame(LongHeader header, Frame frame) {
    return new LongHeader(header.packetType,
                          header.destinationConnectionId,
                          header.sourceConnectionId,
                          header.version,
                          header.packetNumber,
                          header.payload.addFrame(frame));
  }

  private final PacketType packetType;
  private final Optional<ConnectionId> destinationConnectionId;
  private final Optional<ConnectionId> sourceConnectionId;
  private final Version version;
  private final PacketNumber packetNumber;
  private final Payload payload;

  public LongHeader(final PacketType packetType,
                    final Optional<ConnectionId> destinationConnectionId,
                    final Optional<ConnectionId> sourceConnectionId,
                    final Version version,
                    final PacketNumber packetNumber,
                    final Payload payload) {
    this.packetType = packetType; // TODO validate only those valid for LondHeaders are used
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;
    this.version = version;
    this.packetNumber = packetNumber;
    this.payload = payload;
  }

  public PacketType getPacketType() {
    return packetType;
  }

  public Optional<ConnectionId> getDestinationConnectionId() {
    return destinationConnectionId;
  }
  public Optional<ConnectionId> getSourceConnectionId() {
    return sourceConnectionId;
  }

  public Version getVersion() {
    return version;
  }

  public PacketNumber getPacketNumber() {
    return packetNumber;
  }

  public Payload getPayload() {
    return payload;
  }

  public void write(ByteBuf bb, AEAD aead) {
    writePrefix(bb);
    writeSuffix(bb, aead);
  }

  public void writePrefix(ByteBuf bb) {
    int b = (PACKET_TYPE_MASK | packetType.getType()) & 0xFF;
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

  public void writeSuffix(ByteBuf bb, AEAD aead) {
    Varint length = new Varint(payload.getLength()); // TODO packet number length
    length.write(bb);

    packetNumber.writeVarint(bb);

    byte[] aad = new byte[bb.writerIndex()];
    bb.markReaderIndex();
    bb.readBytes(aad);
    bb.resetReaderIndex();

    payload.write(bb, aead, packetNumber, aad);
  }

  @Override
  public String toString() {
    return "LongHeader{" +
            "packetType=" + packetType +
            ", destinationConnectionId=" + Opt.toString(destinationConnectionId) +
            ", sourceConnectionId=" + Opt.toString(sourceConnectionId) +
            ", version=" + version +
            ", packetNumber=" + packetNumber +
            ", payload=" + payload +
            '}';
  }
}
