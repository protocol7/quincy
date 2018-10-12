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
                                     Optional<byte[]> token,
                                     List<Frame> frames) { // TODO validate frame types
    Version version = Version.CURRENT;
    PacketNumber packetNumber = PacketNumber.random();

    Payload payload = new Payload(frames);
    return new InitialPacket(new LongHeader(PacketType.Initial,
            destConnectionId,
            srcConnectionId,
            version,
            packetNumber,
            payload),
            token);
  }

  public static InitialPacket parse(ByteBuf bb) {
    return null;
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
    // Initial packet inserts some extra fields, so needs a custom write method

    bb.writeByte(MARKER);

    header.getVersion().write(bb);

    bb.writeByte(ConnectionId.joinLenghts(header.getDestinationConnectionId(), header.getSourceConnectionId()));

    if (header.getDestinationConnectionId().isPresent()) {
      header.getDestinationConnectionId().get().write(bb);
    }
    if (header.getSourceConnectionId().isPresent()) {
      header.getSourceConnectionId().get().write(bb);
    }

    if (token.isPresent()) {
      byte[] t = token.get();
      new Varint(t.length).write(bb);
      bb.writeBytes(t);
    }

    Varint length = new Varint(8 + header.getPayload().getLength()); // TODO packet number length
    length.write(bb);
    header.getPacketNumber().write(bb);
    header.getPayload().write(bb);

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
}
