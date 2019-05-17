package com.protocol7.quincy.protocol.packets;

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

public class ShortPacket implements FullPacket {

  public static HalfParsedPacket<ShortPacket> parse(final ByteBuf bb, final int connIdLength) {
    final int bbOffset = bb.readerIndex();

    final byte firstByte = bb.readByte();

    final boolean firstBit = (firstByte & 0x80) == 0x80;
    if (firstBit) {
      throw new IllegalArgumentException("First bit must be 0");
    }

    final boolean reserved = (firstByte & 0x40) == 0x40;
    if (!reserved) {
      throw new IllegalArgumentException("Reserved bit must be 1");
    }

    final boolean keyPhase = (firstByte & 0x4) == 0x4;

    final Optional<ConnectionId> connId;
    if (connIdLength > 0) {
      connId = Optional.of(ConnectionId.read(connIdLength, bb));
    } else {
      connId = Optional.empty();
    }

    return new HalfParsedPacket<>() {
      @Override
      public Optional<Version> getVersion() {
        return Optional.empty();
      }

      @Override
      public Optional<ConnectionId> getConnectionId() {
        return connId;
      }

      @Override
      public ShortPacket complete(final AEADProvider aeadProvider) {

        final AEAD aead = aeadProvider.get(EncryptionLevel.OneRtt);

        final int pnOffset = bb.readerIndex();
        final int sampleOffset = pnOffset + 4;

        final byte[] sample = new byte[aead.getSampleLength()];
        bb.getBytes(sampleOffset, sample);

        // get 4 bytes for PN. Might be too long, but we'll handle that below
        final byte[] pn = new byte[4];
        bb.getBytes(pnOffset, pn);

        // decrypt the protected header parts
        try {
          final byte[] decryptedHeader =
              aead.decryptHeader(sample, Bytes.concat(new byte[] {firstByte}, pn), true);

          final byte decryptedFirstByte = decryptedHeader[0];
          final int pnLen = (decryptedFirstByte & 0x3) + 1;

          final byte[] pnBytes = Arrays.copyOfRange(decryptedHeader, 1, 1 + pnLen);

          final long packetNumber = PacketNumber.parse(pnBytes);

          // move reader ahead by what the PN length actually was
          bb.readerIndex(bb.readerIndex() + pnLen);

          final byte[] aad = new byte[bb.readerIndex() - bbOffset];
          bb.getBytes(bbOffset, aad);

          // restore the AAD with the now removed header protected
          aad[0] = decryptedFirstByte;
          for (int i = 0; i < pnBytes.length; i++) {
            aad[pnOffset - bbOffset + i] = pnBytes[i];
          }

          final Payload payload = Payload.parse(bb, bb.readableBytes(), aead, packetNumber, aad);

          return new ShortPacket(keyPhase, connId, packetNumber, payload);
        } catch (final GeneralSecurityException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  public static ShortPacket create(
      final boolean keyPhase,
      final Optional<ConnectionId> connectionId,
      final long packetNumber,
      final Frame... frames) {
    return new ShortPacket(keyPhase, connectionId, packetNumber, new Payload(frames));
  }

  private final boolean keyPhase;
  private final Optional<ConnectionId> connectionId;
  private final long packetNumber;
  private final Payload payload;

  public ShortPacket(
      final boolean keyPhase,
      final Optional<ConnectionId> connectionId,
      final long packetNumber,
      final Payload payload) {
    this.keyPhase = keyPhase;
    this.connectionId = connectionId;
    this.packetNumber = PacketNumber.validate(packetNumber);
    this.payload = payload;
  }

  @Override
  public PacketType getType() {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public FullPacket addFrame(final Frame frame) {
    return new ShortPacket(keyPhase, connectionId, packetNumber, payload.addFrame(frame));
  }

  @Override
  public void write(final ByteBuf bb, final AEAD aead) {
    final int bbOffset = bb.writerIndex();

    byte b = 0;
    b = (byte) (b | 0x40); // reserved must be 1
    if (keyPhase) {
      b = (byte) (b | 0x4);
    }
    // TODO spin bit
    // TODO reserved bits

    final int pnLen = PacketNumber.getLength(packetNumber);

    b = (byte) (b | (pnLen - 1)); // pn length

    bb.writeByte(b);

    connectionId.get().write(bb);

    final int pnOffset = bb.writerIndex();
    final int sampleOffset = pnOffset + 4;

    final byte[] pn = PacketNumber.write(packetNumber, pnLen);
    bb.writeBytes(pn);

    final byte[] aad = new byte[bb.writerIndex() - bbOffset];
    bb.getBytes(bbOffset, aad);

    payload.write(bb, aead, packetNumber, aad);

    final byte[] sample = new byte[aead.getSampleLength()];
    bb.getBytes(sampleOffset, sample);

    final byte firstBýte = bb.getByte(bbOffset);
    final byte[] header = Bytes.concat(new byte[] {firstBýte}, pn);
    try {
      final byte[] encryptedHeader = aead.encryptHeader(sample, header, true);
      bb.setByte(bbOffset, encryptedHeader[0]);
      bb.setBytes(pnOffset, encryptedHeader, 1, encryptedHeader.length - 1);
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public long getPacketNumber() {
    return packetNumber;
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return Optional.empty();
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return connectionId;
  }

  @Override
  public Payload getPayload() {
    return payload;
  }

  @Override
  public String toString() {
    return "ShortPacket{"
        + "keyPhase="
        + keyPhase
        + ", connectionId="
        + connectionId
        + ", packetNumber="
        + packetNumber
        + ", payload="
        + payload
        + '}';
  }
}
