package com.protocol7.nettyquick.protocol;

import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

public class LongPacket implements Packet {

  public static final int PACKET_TYPE_MASK = 0b10000000;

  public static LongPacket parse(ByteBuf bb) {
    byte firstByte = bb.readByte();
    byte ptByte = (byte)((firstByte & (~PACKET_TYPE_MASK)) & 0xFF);
    PacketType packetType = PacketType.read(ptByte);
    ConnectionId connId = ConnectionId.read(bb);
    Version version = Version.read(bb);
    PacketNumber packetNumber = PacketNumber.read(bb);

    Payload payload = Payload.parse(bb);

    return new LongPacket(packetType,
                          connId,
                          version,
                          packetNumber,
                          payload);
  }

  public static LongPacket addFrame(LongPacket packet, Frame frame) {
    return new LongPacket(packet.packetType,
                          packet.connectionId,
                          packet.version,
                          packet.packetNumber,
                          packet.payload.addFrame(frame));
  }

  private final PacketType packetType;
  private final ConnectionId connectionId;
  private final Version version;
  private final PacketNumber packetNumber;
  private final Payload payload;

  public LongPacket(final PacketType packetType, final ConnectionId connectionId, final Version version, final PacketNumber packetNumber, final Payload payload) {
    this.packetType = packetType;
    this.connectionId = connectionId;
    this.version = version;
    this.packetNumber = packetNumber;
    this.payload = payload;
  }

  public PacketType getPacketType() {
    return packetType;
  }

  public ConnectionId getConnectionId() {
    return connectionId;
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

  public void write(ByteBuf bb) {
    int b = (PACKET_TYPE_MASK | packetType.getType()) & 0xFF;
    bb.writeByte(b);

    connectionId.write(bb);
    version.write(bb);
    packetNumber.write(bb);
    payload.write(bb);
  }

  @Override
  public String toString() {
    return "LongPacket{" +
            "packetNumber=" + packetNumber +
            ", packetType=" + packetType +
            ", connectionId=" + connectionId +
            ", payload=" + payload +
            '}';
  }
}
