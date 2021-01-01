package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.tls.EncryptionLevel;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADProvider;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class InitialPacket extends LongHeaderPacket {

  public static InitialPacket create(
      final ConnectionId destConnectionId,
      final ConnectionId srcConnectionId,
      final long packetNumber,
      final Version version,
      final Optional<byte[]> token,
      final Frame... frames) {
    return create(
        destConnectionId, srcConnectionId, packetNumber, version, token, new Payload(frames));
  }

  public static InitialPacket create(
      final ConnectionId destConnectionId,
      final ConnectionId srcConnectionId,
      final long packetNumber,
      final Version version,
      final Optional<byte[]> token,
      final Payload payload) {
    return new InitialPacket(
        destConnectionId, srcConnectionId, version, packetNumber, payload, token);
  }

  public static HalfParsedPacket<InitialPacket> parse(final ByteBuf bb) {
    final int bbOffset = bb.readerIndex();

    final byte firstByte = bb.readByte(); // TODO validate

    final Version version = Version.read(bb);

    final ConnectionId destConnId = ConnectionId.read(bb);
    final ConnectionId srcConnId = ConnectionId.read(bb);

    final int tokenLength = Varint.readAsInt(bb);

    final byte[] tokenBytes = new byte[tokenLength];
    bb.readBytes(tokenBytes);
    final Optional<byte[]> token;
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
      public ConnectionId getConnectionId() {
        return destConnId;
      }

      @Override
      public InitialPacket complete(final AEADProvider aeadProvider) {
        final int length = Varint.readAsInt(bb);

        final AEAD aead = aeadProvider.get(EncryptionLevel.Initial);

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

          return InitialPacket.create(destConnId, srcConnId, packetNumber, version, token, payload);

        } catch (final GeneralSecurityException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  private final Optional<byte[]> token;

  private InitialPacket(
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final Version version,
      final long packetNumber,
      final Payload payload,
      final Optional<byte[]> token) {
    super(
        PacketType.Initial,
        destinationConnectionId,
        sourceConnectionId,
        version,
        packetNumber,
        validateFrames(payload));
    this.token = token;
  }

  @Override
  public InitialPacket addFrame(final Frame frame) {
    return new InitialPacket(
        getDestinationConnectionId(),
        getSourceConnectionId(),
        getVersion(),
        getPacketNumber(),
        getPayload().addFrame(frame),
        token);
  }

  @Override
  public void write(final ByteBuf bb, final AEAD aead) {
    writeInternal(
        bb,
        aead,
        new Consumer<ByteBuf>() {
          @Override
          public void accept(final ByteBuf byteBuf) {
            if (token.isPresent()) {
              final byte[] t = token.get();
              Varint.write(t.length, bb);
              bb.writeBytes(t);
            } else {
              Varint.write(0, bb);
            }
          }
        });
  }

  public Optional<byte[]> getToken() {
    return token;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    final InitialPacket that = (InitialPacket) o;
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
        + "["
        + Hex.hex(token.orElse(new byte[0]))
        + "]"
        + '}';
  }
}
