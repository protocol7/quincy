package com.protocol7.quincy.protocol.packets;

import static java.util.Objects.requireNonNull;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADProvider;
import com.protocol7.quincy.tls.aead.RetryTokenIntegrityTagAEAD;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class RetryPacket implements Packet {

  private static final RetryTokenIntegrityTagAEAD TAG_AEAD = new RetryTokenIntegrityTagAEAD();

  public static RetryPacket createOutgoing(
      final Version version,
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final ConnectionId originalConnectionId,
      final byte[] retryToken) {
    return new RetryPacket(
        version,
        destinationConnectionId,
        sourceConnectionId,
        Optional.of(originalConnectionId),
        retryToken,
        Optional.empty());
  }

  public static HalfParsedPacket<RetryPacket> parse(final ByteBuf bb) {
    final byte b = bb.readByte(); // TODO verify reserved and packet types

    final Version version = Version.read(bb);

    final ConnectionId destConnId = ConnectionId.read(bb);
    final ConnectionId srcConnId = ConnectionId.read(bb);

    final byte[] retryToken = new byte[bb.readableBytes() - 16];
    bb.readBytes(retryToken);

    final byte[] retryTokenIntegrityTag = new byte[16];
    bb.readBytes(retryTokenIntegrityTag);

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
      public RetryPacket complete(final AEADProvider aeadProvider) {
        return new RetryPacket(
            version,
            destConnId,
            srcConnId,
            Optional.empty(),
            retryToken,
            Optional.of(retryTokenIntegrityTag));
      }
    };
  }

  private final Version version;
  private final ConnectionId destinationConnectionId;
  private final ConnectionId sourceConnectionId;
  private final Optional<ConnectionId> originalConnectionId;
  private final byte[] retryToken;
  private final Optional<byte[]> retryTokenIntegrityTag;

  private RetryPacket(
      final Version version,
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final Optional<ConnectionId> originalConnectionId,
      final byte[] retryToken,
      final Optional<byte[]> retryTokenIntegrityTag) {
    this.version = requireNonNull(version);
    this.destinationConnectionId = requireNonNull(destinationConnectionId);
    this.sourceConnectionId = requireNonNull(sourceConnectionId);
    this.originalConnectionId = requireNonNull(originalConnectionId);
    this.retryToken = requireNonNull(retryToken);
    this.retryTokenIntegrityTag = requireNonNull(retryTokenIntegrityTag);
  }

  public Version getVersion() {
    return version;
  }

  public ConnectionId getDestinationConnectionId() {
    return destinationConnectionId;
  }

  @Override
  public void write(final ByteBuf bb, final AEAD aead) {
    if (!originalConnectionId.isPresent()) {
      throw new IllegalStateException("Can't write retry packet without originalConnectionId");
    }

    int b = (PACKET_TYPE_MASK | PacketType.Retry.getType() << 4) & 0xFF;
    b = b | 0x40; // fixed
    bb.writeByte(b);

    version.write(bb);

    ConnectionId.write(destinationConnectionId, bb);
    ConnectionId.write(sourceConnectionId, bb);

    bb.writeBytes(retryToken);

    bb.writeBytes(calculateTokenIntegrityTag(bb, originalConnectionId.get()));
  }

  private byte[] calculateTokenIntegrityTag(
      final ByteBuf header, final ConnectionId originalConnectionId) {

    try {
      return TAG_AEAD.create(retryPseudoPacket(originalConnectionId));
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException("Failed to create RetryTokenIntegrityTag", e);
    }
  }

  public void verify(final ConnectionId originalConnectionId) {
    if (retryTokenIntegrityTag.isEmpty()) {
      throw new IllegalStateException("Can't verify retry packet without retryTokenIntegrityTag");
    }

    try {
      TAG_AEAD.verify(retryPseudoPacket(originalConnectionId), retryTokenIntegrityTag.get());
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException("Failed to verify tag", e);
    }
  }

  private byte[] retryPseudoPacket(final ConnectionId originalConnectionId) {
    final ByteBuf bb = Unpooled.buffer();
    ConnectionId.write(originalConnectionId, bb);

    int b = (PACKET_TYPE_MASK | PacketType.Retry.getType() << 4) & 0xFF;
    b = b | 0x40; // fixed
    bb.writeByte(b);

    version.write(bb);

    ConnectionId.write(destinationConnectionId, bb);
    ConnectionId.write(sourceConnectionId, bb);

    bb.writeBytes(retryToken);

    return Bytes.drainToArray(bb);
  }

  public ConnectionId getSourceConnectionId() {
    return sourceConnectionId;
  }

  public byte[] getRetryToken() {
    return retryToken;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final RetryPacket that = (RetryPacket) o;
    return version == that.version
        && Objects.equals(destinationConnectionId, that.destinationConnectionId)
        && Objects.equals(sourceConnectionId, that.sourceConnectionId)
        && Objects.equals(originalConnectionId, that.originalConnectionId)
        && Arrays.equals(retryToken, that.retryToken);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(version, destinationConnectionId, sourceConnectionId, originalConnectionId);
    result = 31 * result + Arrays.hashCode(retryToken);
    return result;
  }

  @Override
  public String toString() {
    return "RetryPacket{"
        + "version="
        + version
        + ", destinationConnectionId="
        + destinationConnectionId
        + ", sourceConnectionId="
        + sourceConnectionId
        + ", originalConnectionId="
        + originalConnectionId
        + ", retryToken="
        + Hex.hex(retryToken)
        + '}';
  }
}
