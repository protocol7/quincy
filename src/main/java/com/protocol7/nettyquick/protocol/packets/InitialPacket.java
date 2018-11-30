package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquick.EncryptionLevel;
import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.AEADProvider;
import com.protocol7.nettyquick.utils.Opt;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InitialPacket implements FullPacket {

  public static int MARKER = (0x80 | PacketType.Initial.getType()) & 0xFF;

  public static InitialPacket create(
      Optional<ConnectionId> destConnectionId,
      Optional<ConnectionId> srcConnectionId,
      PacketNumber packetNumber,
      Version version,
      Optional<byte[]> token,
      Frame... frames) { // TODO validate frame types
    return create(
        destConnectionId,
        srcConnectionId,
        packetNumber,
        version,
        token,
        ImmutableList.copyOf(frames));
  }

  public static InitialPacket create(
      Optional<ConnectionId> destConnectionId,
      Optional<ConnectionId> srcConnectionId,
      PacketNumber packetNumber,
      Version version,
      Optional<byte[]> token,
      List<Frame> frames) { // TODO validate frame types
    Payload payload = new Payload(frames);
    return new InitialPacket(
        new LongHeader(
            PacketType.Initial, destConnectionId, srcConnectionId, version, packetNumber, payload),
        token);
  }

  public static HalfParsedPacket<InitialPacket> parse(ByteBuf bb) {
    bb.markReaderIndex();

    bb.readByte(); // TODO validate

    Version version = Version.read(bb);

    int cil = bb.readByte() & 0xFF;
    // server 	Long Header{Type: Initial, DestConnectionID: (empty), SrcConnectionID: 0x88f9f1ab,
    // Token: (empty), PacketNumber: 0x1, PacketNumberLen: 2, PayloadLen: 181, Version: TLS dev
    // version (WIP)}

    int dcil = ConnectionId.firstLength(cil);
    int scil = ConnectionId.lastLength(cil);
    Optional<ConnectionId> destConnId = ConnectionId.readOptional(dcil, bb);
    Optional<ConnectionId> srcConnId = ConnectionId.readOptional(scil, bb);

    int tokenLength = Varint.readAsInt(bb);

    byte[] tokenBytes = new byte[tokenLength];
    bb.readBytes(tokenBytes);
    Optional<byte[]> token;
    if (tokenBytes.length > 0) {
      token = Optional.of(tokenBytes);
    } else {
      token = Optional.empty();
    }

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
      public InitialPacket complete(AEADProvider aeadProvider) {
        int length = Varint.readAsInt(bb);

        int beforePnPos = bb.readerIndex();
        PacketNumber packetNumber = PacketNumber.parseVarint(bb);
        int payloadLength = length - (bb.readerIndex() - beforePnPos); // subtract read pn length

        byte[] aad = new byte[bb.readerIndex()];
        bb.resetReaderIndex();
        bb.readBytes(aad);

        AEAD aead = aeadProvider.get(EncryptionLevel.Initial);

        Payload payload = Payload.parse(bb, payloadLength, aead, packetNumber, aad);

        return InitialPacket.create(
            destConnId, srcConnId, packetNumber, version, token, payload.getFrames());
      }
    };
  }

  private final LongHeader header;
  private final Optional<byte[]> token;

  public InitialPacket(LongHeader header, Optional<byte[]> token) {
    this.header = header;
    this.token = token;
  }

  @Override
  public PacketType getType() {
    return PacketType.Initial;
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
      Varint.write(t.length, bb);
      bb.writeBytes(t);
    } else {
      Varint.write(0, bb);
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
  public Payload getPayload() {
    return header.getPayload();
  }

  public Version getVersion() {
    return header.getVersion();
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InitialPacket that = (InitialPacket) o;
    return Objects.equals(header, that.header) && Objects.equals(token, that.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(header, token);
  }

  @Override
  public String toString() {
    return "InitialPacket{" + "header=" + header + ", token=" + Opt.toStringBytes(token) + '}';
  }
}
