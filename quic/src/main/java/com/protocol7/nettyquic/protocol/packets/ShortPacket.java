package com.protocol7.nettyquic.protocol.packets;

import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.tls.EncryptionLevel;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADProvider;
import com.protocol7.nettyquic.utils.Bytes;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;

public class ShortPacket implements FullPacket {

  public static HalfParsedPacket<ShortPacket> parse(ByteBuf bb, int connIdLength) {
    int bbOffset = bb.readerIndex();

    byte firstByte = bb.readByte();

    boolean firstBit = (firstByte & 0x80) == 0x80;
    if (firstBit) {
      throw new IllegalArgumentException("First bit must be 0");
    }

    boolean reserved = (firstByte & 0x40) == 0x40;
    if (!reserved) {
      throw new IllegalArgumentException("Reserved bit must be 1");
    }

    boolean keyPhase = (firstByte & 0x4) == 0x4;

    Optional<ConnectionId> connId;
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
      public ShortPacket complete(AEADProvider aeadProvider) {

        AEAD aead = aeadProvider.get(EncryptionLevel.OneRtt);

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
              aead.decryptHeader(sample, Bytes.concat(new byte[] {firstByte}, pn), true);

          byte decryptedFirstByte = decryptedHeader[0];
          int pnLen = (decryptedFirstByte & 0x3) + 1;

          byte[] pnBytes = Arrays.copyOfRange(decryptedHeader, 1, 1 + pnLen);

          PacketNumber packetNumber = PacketNumber.parse(pnBytes);

          // move reader ahead by what the PN length actually was
          bb.readerIndex(bb.readerIndex() + pnLen);

          byte[] aad = new byte[bb.readerIndex() - bbOffset];
          bb.getBytes(bbOffset, aad);

          // restore the AAD with the now removed header protected
          aad[0] = decryptedFirstByte;
          for (int i = 0; i < pnBytes.length; i++) {
            aad[pnOffset - bbOffset + i] = pnBytes[i];
          }

          Payload payload = Payload.parse(bb, bb.readableBytes(), aead, packetNumber, aad);

          return new ShortPacket(keyPhase, connId, packetNumber, payload);
        } catch (GeneralSecurityException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  public static ShortPacket create(
      boolean keyPhase,
      Optional<ConnectionId> connectionId,
      PacketNumber packetNumber,
      Frame... frames) {
    return new ShortPacket(keyPhase, connectionId, packetNumber, new Payload(frames));
  }

  private final boolean keyPhase;
  private final Optional<ConnectionId> connectionId;
  private final PacketNumber packetNumber;
  private final Payload payload;

  public ShortPacket(
      boolean keyPhase,
      Optional<ConnectionId> connectionId,
      PacketNumber packetNumber,
      Payload payload) {
    this.keyPhase = keyPhase;
    this.connectionId = connectionId;
    this.packetNumber = packetNumber;
    this.payload = payload;
  }

  @Override
  public PacketType getType() {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Packet addFrame(Frame frame) {
    return new ShortPacket(keyPhase, connectionId, packetNumber, payload.addFrame(frame));
  }

  @Override
  public void write(ByteBuf bb, AEAD aead) {
    int bbOffset = bb.writerIndex();

    byte b = 0;
    b = (byte) (b | 0x40); // reserved must be 1
    if (keyPhase) {
      b = (byte) (b | 0x4);
    }
    // TODO spin bit
    // TODO reserved bits

    int pnLen = packetNumber.getLength();

    b = (byte) (b | (pnLen - 1)); // pn length

    bb.writeByte(b);

    connectionId.get().write(bb);

    int pnOffset = bb.writerIndex();
    int sampleOffset = pnOffset + 4;

    byte[] pn = packetNumber.write(pnLen);
    bb.writeBytes(pn);

    byte[] aad = new byte[bb.writerIndex() - bbOffset];
    bb.getBytes(bbOffset, aad);

    payload.write(bb, aead, packetNumber, aad);

    byte[] sample = new byte[aead.getSampleLength()];
    bb.getBytes(sampleOffset, sample);

    byte firstBýte = bb.getByte(bbOffset);
    byte[] header = Bytes.concat(new byte[] {firstBýte}, pn);
    try {
      byte[] encryptedHeader = aead.encryptHeader(sample, header, true);
      bb.setByte(bbOffset, encryptedHeader[0]);
      bb.setBytes(pnOffset, encryptedHeader, 1, encryptedHeader.length - 1);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public PacketNumber getPacketNumber() {
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
