package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.AEAD;
import com.protocol7.nettyquick.tls.AEADProvider;
import com.protocol7.nettyquick.utils.Opt;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Optional;

public class InitialPacket implements FullPacket {

  public static int MARKER = (0x80 | PacketType.Initial.getType()) & 0xFF;

  public static InitialPacket create(Optional<ConnectionId> destConnectionId,
                                     Optional<ConnectionId> srcConnectionId,
                                     PacketNumber packetNumber,
                                     Version version,
                                     Optional<byte[]> token,
                                     Frame... frames) { // TODO validate frame types
    return create(destConnectionId, srcConnectionId, packetNumber, version, token, ImmutableList.copyOf(frames));
  }

  public static InitialPacket create(Optional<ConnectionId> destConnectionId,
                                     Optional<ConnectionId> srcConnectionId,
                                     PacketNumber packetNumber,
                                     Version version,
                                     Optional<byte[]> token,
                                     List<Frame> frames) { // TODO validate frame types
    UnprotectedPayload payload = new UnprotectedPayload(frames);
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

  public static InitialPacket parse(ByteBuf bb, AEADProvider aeadProvider) {
    bb.markReaderIndex();

    bb.readByte(); // TODO validate

    Version version = Version.read(bb);

    int cil = bb.readByte() & 0xFF;
    // server 	Long Header{Type: Initial, DestConnectionID: (empty), SrcConnectionID: 0x88f9f1ab, Token: (empty), PacketNumber: 0x1, PacketNumberLen: 2, PayloadLen: 181, Version: TLS dev version (WIP)}

    int dcil = ConnectionId.firstLength(cil);
    int scil = ConnectionId.lastLength(cil);
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


    PacketNumber packetNumber = PacketNumber.parseVarint(bb);
    int payloadLength = length; // TODO pn length

    byte[] aad = new byte[bb.readerIndex()];
    bb.resetReaderIndex();
    bb.readBytes(aad);

    AEAD aead = aeadProvider.forConnection(destConnId, PacketType.Initial);

    UnprotectedPayload payload = UnprotectedPayload.parse(bb, payloadLength, aead, packetNumber, aad);

    return InitialPacket.create(
            destConnId,
            srcConnId,
            packetNumber,
            version,
            token,
            payload.getFrames());
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
  public void write(ByteBuf bb, AEAD aead) {
    header.writePrefix(bb);

    if (token.isPresent()) {
      byte[] t = token.get();
      new Varint(t.length).write(bb);
      bb.writeBytes(t);
    } else {
      new Varint(0).write(bb);
    }

    header.writeSuffix(bb, aead);
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
  public UnprotectedPayload getPayload() {
    return header.getPayload();
  }

  public Version getVersion() {
    return header.getVersion();
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  @Override
  public String toString() {
    return "InitialPacket{" +
            "header=" + header +
            ", token=" + Opt.toStringBytes(token) +
            '}';
  }
}
