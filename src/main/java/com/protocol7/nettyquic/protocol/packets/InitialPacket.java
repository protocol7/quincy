package com.protocol7.nettyquic.protocol.packets;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADProvider;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class InitialPacket extends LongHeaderPacket {

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
        destConnectionId, srcConnectionId, version, packetNumber, payload, token);
  }

  public static HalfParsedPacket<InitialPacket> parse(ByteBuf bb) {
    bb.markReaderIndex();

    byte firstByte = bb.readByte(); // TODO validate
    int pnLen = (firstByte & 0x3) + 1;

    Version version = Version.read(bb);

    int cil = bb.readByte() & 0xFF;
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
        PacketNumber packetNumber = PacketNumber.parse(bb, pnLen);
        int payloadLength =
            length - (bb.readerIndex() - beforePnPos); // subtract fromByte pn length

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

  private final Optional<byte[]> token;

  public InitialPacket(
      Optional<ConnectionId> destinationConnectionId,
      Optional<ConnectionId> sourceConnectionId,
      Version version,
      PacketNumber packetNumber,
      Payload payload,
      Optional<byte[]> token) {
    super(
        PacketType.Initial,
        destinationConnectionId,
        sourceConnectionId,
        version,
        packetNumber,
        payload);
    this.token = token;
  }

  @Override
  public InitialPacket addFrame(Frame frame) {
    return new InitialPacket(
        getDestinationConnectionId(),
        getSourceConnectionId(),
        getVersion(),
        getPacketNumber(),
        getPayload().addFrame(frame),
        token);
  }

  @Override
  public void write(ByteBuf bb, AEAD aead) {
    writePrefix(bb);

    if (token.isPresent()) {
      byte[] t = token.get();
      Varint.write(t.length, bb);
      bb.writeBytes(t);
    } else {
      Varint.write(0, bb);
    }

    writeSuffix(bb, aead);
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    InitialPacket that = (InitialPacket) o;
    return Objects.equals(token, that.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), token);
  }

  @Override
  public String toString() {
    return "InitialPacket{"
        + "packetType="
        + getType()
        + ", destinationConnectionId="
        + getDestinationConnectionId()
        + ", sourceConnectionId="
        + getSourceConnectionId()
        + ", version="
        + getVersion()
        + ", packetNumber="
        + getPacketNumber()
        + ", payload="
        + getPayload()
        + ", token="
        + token
        + '}';
  }
}
