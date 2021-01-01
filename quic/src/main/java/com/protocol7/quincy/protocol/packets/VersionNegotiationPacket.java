package com.protocol7.quincy.protocol.packets;

import com.protocol7.quincy.protocol.ConnectionId;
import com.protocol7.quincy.protocol.Version;
import com.protocol7.quincy.tls.aead.AEAD;
import com.protocol7.quincy.tls.aead.AEADProvider;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VersionNegotiationPacket implements Packet {

  private static final int MARKER = 0b10000000;

  public static HalfParsedPacket<VersionNegotiationPacket> parse(final ByteBuf bb) {
    final byte marker = bb.readByte();
    if ((marker & MARKER) != MARKER) {
      throw new IllegalArgumentException("Illegal marker");
    }

    final Version version = Version.read(bb);

    if (version != Version.VERSION_NEGOTIATION) {
      throw new IllegalArgumentException("Invalid version");
    }

    final ConnectionId destConnId = ConnectionId.read(bb);
    final ConnectionId srcConnId = ConnectionId.read(bb);

    final List<Version> supported = new ArrayList<>();
    while (bb.isReadable()) {
      final Version v = Version.read(bb);
      if (v != Version.UNKNOWN) {
        supported.add(v);
      }
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
      public VersionNegotiationPacket complete(final AEADProvider aeadProvider) {
        return new VersionNegotiationPacket(destConnId, srcConnId, supported);
      }
    };
  }

  private final ConnectionId destinationConnectionId;
  private final ConnectionId sourceConnectionId;
  private final List<Version> supportedVersions;

  public VersionNegotiationPacket(
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final Version... supportedVersions) {
    this(destinationConnectionId, sourceConnectionId, List.of(supportedVersions));
  }

  public VersionNegotiationPacket(
      final ConnectionId destinationConnectionId,
      final ConnectionId sourceConnectionId,
      final List<Version> supportedVersions) {
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;

    if (supportedVersions.isEmpty()) {
      throw new IllegalArgumentException("Supported versions must not be empty");
    }
    this.supportedVersions = supportedVersions;
  }

  @Override
  public void write(final ByteBuf bb, final AEAD notUsed) {
    int marker = Rnd.rndInt() & 0xFF;
    marker |= MARKER;
    bb.writeByte(marker);

    Version.VERSION_NEGOTIATION.write(bb);

    ConnectionId.write(destinationConnectionId, bb);
    ConnectionId.write(sourceConnectionId, bb);

    for (final Version version : supportedVersions) {
      version.write(bb);
    }
  }

  @Override
  public ConnectionId getSourceConnectionId() {
    return sourceConnectionId;
  }

  @Override
  public ConnectionId getDestinationConnectionId() {
    return destinationConnectionId;
  }

  public List<Version> getSupportedVersions() {
    return supportedVersions;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final VersionNegotiationPacket that = (VersionNegotiationPacket) o;
    return Objects.equals(destinationConnectionId, that.destinationConnectionId)
        && Objects.equals(sourceConnectionId, that.sourceConnectionId)
        && Objects.equals(supportedVersions, that.supportedVersions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(destinationConnectionId, sourceConnectionId, supportedVersions);
  }
}
