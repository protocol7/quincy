package com.protocol7.nettyquic.protocol.packets;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADProvider;
import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Opt;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import java.util.Arrays;
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

        AEAD aead = aeadProvider.get(EncryptionLevel.Initial);

        int pnOffset = bb.readerIndex();
        int sampleOffset = pnOffset + 4;

        byte[] sample = new byte[aead.getSampleLength()];

        bb.getBytes(sampleOffset, sample);

        // get 4 bytes for PN. Might be too long, but we'll handle that below
        byte[] pn = new byte[4];
        bb.getBytes(pnOffset, pn);

        // decrypt the protected header parts
        try {
          byte[] decryptedHeader =
              aead.decryptHeader(sample, Bytes.concat(new byte[] {firstByte}, pn), false);

          byte decryptedFirstByte = decryptedHeader[0];
          int pnLen = (decryptedFirstByte & 0x3) + 1;

          byte[] pnBytes = Arrays.copyOfRange(decryptedHeader, 1, 1 + pnLen);

          PacketNumber packetNumber = PacketNumber.parse(pnBytes);

          // move reader ahead by what the PN length actually was
          bb.readerIndex(bb.readerIndex() + pnLen);
          int payloadLength = length - (bb.readerIndex() - pnOffset); // subtract fromByte pn length

          byte[] aad = new byte[bb.readerIndex()];
          bb.resetReaderIndex();
          bb.readBytes(aad);

          // restore the AAD with the now removed header protected
          aad[0] = decryptedFirstByte;
          for (int i = 0; i < pnBytes.length; i++) {
            aad[pnOffset + i] = pnBytes[i];
          }

          Payload payload = Payload.parse(bb, payloadLength, aead, packetNumber, aad);

          return InitialPacket.create(
              destConnId, srcConnId, packetNumber, version, token, payload.getFrames());

        } catch (GeneralSecurityException e) {
          throw new RuntimeException(e);
        }
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
        + Opt.toStringBytes(token)
        + '}';
  }
}
