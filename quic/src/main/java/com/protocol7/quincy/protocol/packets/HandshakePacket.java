package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADProvider;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;

public class HandshakePacket extends LongHeaderPacket {

  public static HandshakePacket create(
      final ConnectionId destConnectionId,
      final ConnectionId srcConnectionId,
      final long packetNumber,
      final Version version,
      final Frame... frames) {
    final Payload payload = new Payload(frames);
    return new HandshakePacket(destConnectionId, srcConnectionId, version, packetNumber, payload);
  }

  public static HalfParsedPacket<HandshakePacket> parse(final ByteBuf bb) {
    // TODO merge with InitialPacket parsing
    // TODO validate marker

    final int bbOffset = bb.readerIndex();

    final byte firstByte = bb.readByte();
    final byte ptByte = (byte) ((firstByte & 0x30) >> 4);
    final PacketType packetType = PacketType.fromByte(ptByte);
    if (packetType != PacketType.Handshake) {
      throw new IllegalArgumentException("Invalid packet type");
    }

    final Version version = Version.read(bb);

    final ConnectionId destConnId = ConnectionId.read(bb);
    final ConnectionId srcConnId = ConnectionId.read(bb);

    return new HalfParsedPacket<>() {
      @Override
      public Optional<Version> getVersion() {
        return Optional.of(version);
      }

      @Override
      public ConnectionId getConnectionId() {
        return destConnId;
      }

      @Override
      public HandshakePacket complete(final AEADProvider aeadProvider) {
        final int length = Varint.readAsInt(bb);

        final AEAD aead = aeadProvider.get(EncryptionLevel.Handshake);

        final int pnOffset = bb.readerIndex();
        // sampling assumes a fixed 4 byte PN length
        final int sampleOffset = pnOffset + 4;

        final int sampleLength = aead.getSampleLength();

        // sampling assumes a fixed 4 byte PN length
        if (length + 4 < sampleLength) {
          throw new IllegalArgumentException("Packet too short to sample");
        }

        final byte[] sample = new byte[sampleLength];

        bb.getBytes(sampleOffset, sample);

        // get 4 bytes for PN. Might be too long, but we'll handle that below
        final byte[] pn = new byte[4];
        bb.getBytes(pnOffset, pn);

        // decrypt the protected header parts
        try {
          final byte[] decryptedHeader =
              aead.decryptHeader(sample, Bytes.concat(new byte[] {firstByte}, pn), false);

          final byte decryptedFirstByte = decryptedHeader[0];
          final int pnLen = (decryptedFirstByte & 0x3) + 1;

          final byte[] pnBytes = Arrays.copyOfRange(decryptedHeader, 1, 1 + pnLen);

          final long packetNumber = PacketNumber.parse(pnBytes);

          // move reader ahead by what the PN length actually was
          bb.readerIndex(bb.readerIndex() + pnLen);
          final int payloadLength = length - pnLen; // subtract parsed pn length

          final byte[] aad = new byte[bb.readerIndex() - bbOffset];
          bb.getBytes(bbOffset, aad);

          // restore the AAD with the now removed header protected
          aad[0] = decryptedFirstByte;
          for (int i = 0; i < pnBytes.length; i++) {
            aad[pnOffset - bbOffset + i] = pnBytes[i];
          }

          final Payload payload = Payload.parse(bb, payloadLength, aead, packetNumber, aad);

          return new HandshakePacket(destConnId, srcConnId, version, packetNumber, payload);
        } catch (final GeneralSecurityException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private HandshakePacket(
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final Version version,
      final long packetNumber,
      final Payload payload) {
    super(
        PacketType.Handshake,
        destinationConnectionId,
        sourceConnectionId,
        version,
        packetNumber,
        validateFrames(payload));
  }

  @Override
  public HandshakePacket addFrame(final Frame frame) {
    return new HandshakePacket(
        getDestinationConnectionId(),
        getSourceConnectionId(),
        getVersion(),
        getPacketNumber(),
        getPayload().addFrame(frame));
  }

  @Override
  public void write(final ByteBuf bb, final AEAD aead) {
    writeInternal(bb, aead, byteBuf -> {});
  }

  @Override
  public String toString() {
    return "HandshakePacket{"
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
        + '}';
  }
}
