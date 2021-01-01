package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.Varint;
import com.protocol7.quincy.protocol.*;
import com.protocol7.quincy.protocol.frames.AckFrame;
import com.protocol7.quincy.protocol.frames.ApplicationCloseFrame;
import com.protocol7.quincy.protocol.frames.ConnectionCloseFrame;
import com.protocol7.quincy.protocol.frames.CryptoFrame;
import com.protocol7.quincy.protocol.frames.Frame;
import com.protocol7.quincy.protocol.frames.PaddingFrame;
import com.protocol7.quincy.protocol.frames.PingFrame;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class LongHeaderPacket implements FullPacket {

  /** Validate frame types for initial and handshake packets */
  protected static Payload validateFrames(final Payload payload) {
    for (final Frame frame : payload.getFrames()) {
      if (frame instanceof CryptoFrame
          || frame instanceof AckFrame
          || frame instanceof PingFrame
          || frame instanceof PaddingFrame
          || frame instanceof ConnectionCloseFrame
          || frame instanceof ApplicationCloseFrame) {
        // ok
      } else {
        throw new IllegalArgumentException("Illegal frame type for packet type: " + frame);
      }
    }
    return payload;
  }

  private final PacketType packetType;
  private final ConnectionId destinationConnectionId;
  private final ConnectionId sourceConnectionId;
  private final Version version;
  private final long packetNumber;
  private final Payload payload;

  public LongHeaderPacket(
      final PacketType packetType,
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final Version version,
      final long packetNumber,
      final Payload payload) {
    this.packetType = packetType;
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;
    this.version = version;
    this.packetNumber = PacketNumber.validate(packetNumber);
    this.payload = payload;
  }

  @Override
  public PacketType getType() {
    return packetType;
  }

  @Override
  public ConnectionId getDestinationConnectionId() {
    return destinationConnectionId;
  }

  @Override
  public ConnectionId getSourceConnectionId() {
    return sourceConnectionId;
  }

  public Version getVersion() {
    return version;
  }

  @Override
  public long getPacketNumber() {
    return packetNumber;
  }

  @Override
  public Payload getPayload() {
    return payload;
  }

  protected void writeInternal(
      final ByteBuf bb, final AEAD aead, final Consumer<ByteBuf> tokenWriter) {
    final int bbOffset = bb.writerIndex();

    int b = (PACKET_TYPE_MASK | packetType.getType() << 4) & 0xFF;
    b = b | 0x40; // fixed

    final byte[] pn = PacketNumber.write(packetNumber);
    b = (byte) (b | (pn.length - 1)); // pn length
    bb.writeByte(b);

    version.write(bb);

    ConnectionId.write(destinationConnectionId, bb);
    ConnectionId.write(sourceConnectionId, bb);

    tokenWriter.accept(bb);

    final int payloadLength = payload.calculateLength();
    final int length = payloadLength + pn.length;
    Varint.write(length, bb);

    final int pnOffset = bb.writerIndex();
    // sampling assumes a fixed 4 byte PN length
    final int sampleOffset = bb.writerIndex() + 4;

    bb.writeBytes(pn);

    final byte[] aad = new byte[bb.writerIndex() - bbOffset];
    bb.getBytes(bbOffset, aad);

    payload.write(bb, aead, packetNumber, aad);

    final int sampleLength = aead.getSampleLength();

    // sampling assumes a fixed 4 byte PN length
    if (payloadLength + 4 < sampleLength) {
      throw new IllegalArgumentException("Packet too short to sample");
    }

    final byte[] sample = new byte[sampleLength];

    bb.getBytes(sampleOffset, sample);

    final byte firstBýte = bb.getByte(bbOffset);
    final byte[] header = Bytes.concat(new byte[] {firstBýte}, pn);
    try {
      final byte[] encryptedHeader = aead.encryptHeader(sample, header, false);
      bb.setByte(bbOffset, encryptedHeader[0]);
      bb.setBytes(pnOffset, encryptedHeader, 1, encryptedHeader.length - 1);
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final LongHeaderPacket that = (LongHeaderPacket) o;
    return packetType == that.packetType
        && Objects.equals(destinationConnectionId, that.destinationConnectionId)
        && Objects.equals(sourceConnectionId, that.sourceConnectionId)
        && version == that.version
        && Objects.equals(packetNumber, that.packetNumber)
        && Objects.equals(payload, that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        packetType, destinationConnectionId, sourceConnectionId, version, packetNumber, payload);
  }
}
