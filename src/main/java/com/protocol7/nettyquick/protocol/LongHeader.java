package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.utils.Bytes;
import com.protocol7.nettyquick.utils.Opt;
import io.netty.buffer.ByteBuf;

import java.util.Optional;

public class LongHeader implements Header {

  public static final int PACKET_TYPE_MASK = 0b10000000;

  public static LongHeader parse(ByteBuf bb, boolean pnVarint) {
    System.out.println(bb.readableBytes());

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

    PacketNumber packetNumber;
    if (pnVarint) {
      packetNumber = new PacketNumber(Varint.read(bb));
    } else {
      packetNumber = PacketNumber.read2(bb, PacketNumber.MIN);
    }

    System.out.println(destConnId);
    System.out.println(srcConnId);
    System.out.println(packetNumber);

    int payloadLength = length; // TODO pn length
    System.out.println(payloadLength);
    System.out.println(bb.readableBytes());

    UnprotectedPayload payload = UnprotectedPayload.parse(bb, payloadLength);

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
  private final UnprotectedPayload payload;

  public LongHeader(final PacketType packetType,
                    final Optional<ConnectionId> destinationConnectionId,
                    final Optional<ConnectionId> sourceConnectionId,
                    final Version version,
                    final PacketNumber packetNumber,
                    final UnprotectedPayload payload) {
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

  public UnprotectedPayload getPayload() {
    return payload;
  }

  public void write(ByteBuf bb) {
    writePrefix(bb);
    writeSuffix(bb);
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

  public void writeSuffix(ByteBuf bb) {
    Varint length = new Varint(payload.getLength()); // TODO packet number length
    length.write(bb);
    packetNumber.asVarint().write(bb);
    payload.write(bb);
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
