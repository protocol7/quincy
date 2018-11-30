package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.EncryptionLevel;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.AEADProvider;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class HandshakePacket implements FullPacket {

  public static int MARKER = 0x80 | PacketType.Handshake.getType();

  public static HandshakePacket create(
      Optional<ConnectionId> destConnectionId,
      Optional<ConnectionId> srcConnectionId,
      PacketNumber packetNumber,
      Version version,
      Frame... frames) {
    return create(destConnectionId, srcConnectionId, packetNumber, version, Arrays.asList(frames));
  }

  public static HandshakePacket create(
      Optional<ConnectionId> destConnectionId,
      Optional<ConnectionId> srcConnectionId,
      PacketNumber packetNumber,
      Version version,
      List<Frame> frames) {
    Payload payload = new Payload(frames);
    return new HandshakePacket(
        new LongHeader(
            PacketType.Handshake,
            destConnectionId,
            srcConnectionId,
            version,
            packetNumber,
            payload));
  }

  public static HalfParsedPacket<HandshakePacket> parse(ByteBuf bb) {
    // TODO validate marker

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

    return new HalfParsedPacket<>() {
      @Override
      public Optional<Version> getVersion() {
        return Optional.of(version);
      }

      @Override
      public Optional<ConnectionId> getConnectionId() {
        return destConnId;
      }

      @Override
      public HandshakePacket complete(AEADProvider aeadProvider) {
        int length = (int) Varint.readAsLong(bb);
        int beforePnPos = bb.readerIndex();
        PacketNumber packetNumber = PacketNumber.parseVarint(bb);

        int payloadLength = length - (bb.readerIndex() - beforePnPos); // remove length read for pn

        byte[] aad = new byte[bb.readerIndex()];
        bb.resetReaderIndex();
        bb.readBytes(aad);

        AEAD aead = aeadProvider.get(EncryptionLevel.Handshake);

        Payload payload = Payload.parse(bb, payloadLength, aead, packetNumber, aad);

        LongHeader header =
            new LongHeader(packetType, destConnId, srcConnId, version, packetNumber, payload);

        return new HandshakePacket(header);
      }
    };
  }

  private final LongHeader header;

  private HandshakePacket(LongHeader header) {
    this.header = header;
  }

  public Version getVersion() {
    return header.getVersion();
  }

  @Override
  public PacketType getType() {
    return PacketType.Handshake;
  }

  @Override
  public Packet addFrame(Frame frame) {
    return new HandshakePacket(LongHeader.addFrame(header, frame));
  }

  @Override
  public void write(ByteBuf bb, AEAD aead) {
    header.write(bb, aead);
  }

  @Override
  public PacketNumber getPacketNumber() {
    return header.getPacketNumber();
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return header.getSourceConnectionId();
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return header.getDestinationConnectionId();
  }

  @Override
  public Payload getPayload() {
    return header.getPayload();
  }

  @Override
  public String toString() {
    return "HandshakePacket{" + "header=" + header + '}';
  }
}
