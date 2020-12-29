package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADProvider;
import com.protocol7.quincy.tls.aead.RetryTokenIntegrityTagAEAD;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import com.protocol7.quincy.utils.Opt;
import com.protocol7.quincy.utils.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class RetryPacket implements Packet {

  private static final RetryTokenIntegrityTagAEAD TAG_AEAD = new RetryTokenIntegrityTagAEAD();

  public static HalfParsedPacket<RetryPacket> parse(final ByteBuf bb) {
    final byte b = bb.readByte(); // TODO verify reserved and packet types

    final Version version = Version.read(bb);

    final Pair<Optional<ConnectionId>, Optional<ConnectionId>> cids = ConnectionId.readPair(bb);

    final Optional<ConnectionId> destConnId = cids.getFirst();
    final Optional<ConnectionId> srcConnId = cids.getSecond();

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
      public Optional<ConnectionId> getConnectionId() {
        return destConnId;
      }

      @Override
      public RetryPacket complete(final AEADProvider aeadProvider) {
        return new RetryPacket(
            version, destConnId, srcConnId, Optional.empty(), retryToken, retryTokenIntegrityTag);
      }
    };
  }

  private final Version version;
  private final Optional<ConnectionId> destinationConnectionId;
  private final Optional<ConnectionId> sourceConnectionId;
  private final Optional<ConnectionId> originalConnectionId;
  private final byte[] retryToken;
  private final byte[] retryTokenIntegrityTag;

  public RetryPacket(
      final Version version,
      final Optional<ConnectionId> destinationConnectionId,
      final Optional<ConnectionId> sourceConnectionId,
      final Optional<ConnectionId> originalConnectionId,
      final byte[] retryToken,
      final byte[] retryTokenIntegrityTag) {
    this.version = version;
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;
    this.originalConnectionId = originalConnectionId;
    this.retryToken = retryToken;
    this.retryTokenIntegrityTag = retryTokenIntegrityTag;
  }

  public Version getVersion() {
    return version;
  }

  public Optional<ConnectionId> getDestinationConnectionId() {
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

    ConnectionId.write(destinationConnectionId, sourceConnectionId, bb);

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
    if (retryTokenIntegrityTag == null) {
      throw new IllegalStateException("Can't verify retry packet without retryTokenIntegrityTag");
    }

    try {
      TAG_AEAD.verify(retryPseudoPacket(originalConnectionId), retryTokenIntegrityTag);
    } catch (final GeneralSecurityException e) {
      throw new RuntimeException("Failed to verify tag", e);
    }
  }

  private byte[] retryPseudoPacket(final ConnectionId originalConnectionId) {
    final ByteBuf bb = Unpooled.buffer();
    ConnectionId.write(Optional.of(originalConnectionId), bb);

    int b = (PACKET_TYPE_MASK | PacketType.Retry.getType() << 4) & 0xFF;
    b = b | 0x40; // fixed
    bb.writeByte(b);

    version.write(bb);

    ConnectionId.write(destinationConnectionId, sourceConnectionId, bb);

    bb.writeBytes(retryToken);

    return Bytes.drainToArray(bb);
  }

  public Optional<ConnectionId> getSourceConnectionId() {
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
        + Opt.toString(destinationConnectionId)
        + ", sourceConnectionId="
        + Opt.toString(sourceConnectionId)
        + ", originalConnectionId="
        + originalConnectionId
        + ", retryToken="
        + Hex.hex(retryToken)
        + '}';
  }
}
