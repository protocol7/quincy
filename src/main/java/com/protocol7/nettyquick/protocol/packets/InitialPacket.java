package com.protocol7.nettyquick.protocol.packets;

import java.util.List;
import java.util.Optional;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.LongHeader;
import com.protocol7.nettyquick.protocol.frames.Frame;
import io.netty.buffer.ByteBuf;

public class InitialPacket implements Packet {

  public static int MARKER = 0x80 | 0x7f;

  public static InitialPacket create(Optional<ConnectionId> destConnectionId,
                                     Optional<ConnectionId> srcConnectionId,
                                     PacketNumber packetNumber,
                                     Version version,
                                     Optional<byte[]> token,
                                     List<Frame> frames) { // TODO validate frame types
    Payload payload = new Payload(frames);
    return new InitialPacket(new LongHeader(PacketType.Initial,
            destConnectionId,
            srcConnectionId,
            version,
            packetNumber,
            payload),
            token);
  }

  public static InitialPacket create(Optional<ConnectionId> destConnectionId,
                                     Optional<ConnectionId> srcConnectionId,
                                     Optional<byte[]> token,
                                     List<Frame> frames) { // TODO validate frame types
    Version version = Version.CURRENT;
    PacketNumber packetNumber = PacketNumber.random();
    return create(destConnectionId, srcConnectionId, packetNumber, version, token, frames);

  }

  public static InitialPacket parse(ByteBuf bb) {
    bb.readByte(); // TODO validate

    Version version = Version.read(bb);

    int cil = bb.readByte() & 0xFF;
    int dcil = ((cil & 0b11110000) >> 4) + 3;
    int scil = (cil & 0b00001111) + 3;

    Optional<ConnectionId> destConnId = ConnectionId.readOptional(dcil, bb);
    Optional<ConnectionId> srcConnId = ConnectionId.readOptional(scil, bb);

    Varint tokenLength = Varint.read(bb);
    byte[] tokenBytes = new byte[(int) tokenLength.getValue()];
    bb.readBytes(tokenBytes);
    Optional<byte[]> token;
    if (tokenBytes.length > 0) {
      token = Optional.of(tokenBytes);
    } else {
      token = Optional.empty();
    }

    int length = (int) Varint.read(bb).getValue();

    PacketNumber packetNumber = PacketNumber.read(bb);
    int payloadLength = length - 8; // TODO pn length

    Payload payload = Payload.parse(bb); // TOOO length

    return InitialPacket.create(destConnId, srcConnId, token, payload.getFrames());
  }

  private final LongHeader header;
  private final Optional<byte[]> token;


  public InitialPacket(LongHeader header, Optional<byte[]> token) {
    this.header = header;
    this.token = token;
  }


  @Override
  public Packet addFrame(Frame frame) {
    return new InitialPacket(LongHeader.addFrame(header, frame), token);
  }

  @Override
  public void write(ByteBuf bb) {
    header.writePrefix(bb);

    if (token.isPresent()) {
      byte[] t = token.get();
      new Varint(t.length).write(bb);
      bb.writeBytes(t);
    }

    header.writeSuffix(bb);
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
  public PacketType getPacketType() {
    return header.getPacketType();
  }

  @Override
  public Payload getPayload() {
    return header.getPayload();
  }

  public Version getVersion() {
    return header.getVersion();
  }

  public Optional<byte[]> getToken() {
    return token;
  }
}
