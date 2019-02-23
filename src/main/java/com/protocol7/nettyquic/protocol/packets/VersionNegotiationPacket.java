package com.protocol7.nettyquic.protocol.packets;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADProvider;
import com.protocol7.nettyquic.utils.Pair;
import com.protocol7.nettyquic.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VersionNegotiationPacket implements Packet {

  private static final int MARKER = 0b10000000;

  public static HalfParsedPacket<VersionNegotiationPacket> parse(ByteBuf bb) {
    final byte marker = bb.readByte();
    if ((marker & MARKER) != MARKER) {
      throw new IllegalArgumentException("Illegal marker");
    }

    final Version version = Version.read(bb);

    if (version != Version.VERSION_NEGOTIATION) {
      throw new IllegalArgumentException("Invalid version");
    }

    final Pair<Optional<ConnectionId>, Optional<ConnectionId>> cids = ConnectionId.readPair(bb);

    final Optional<ConnectionId> destConnId = cids.getFirst();
    final Optional<ConnectionId> srcConnId = cids.getSecond();

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
      public Optional<ConnectionId> getConnectionId() {
        return destConnId;
      }

      @Override
      public VersionNegotiationPacket complete(AEADProvider aeadProvider) {
        return new VersionNegotiationPacket(destConnId, srcConnId, supported);
      }
    };
  }

  private final Optional<ConnectionId> destinationConnectionId;
  private final Optional<ConnectionId> sourceConnectionId;
  private final List<Version> supportedVersions;

  public VersionNegotiationPacket(
      final Optional<ConnectionId> destinationConnectionId,
      final Optional<ConnectionId> sourceConnectionId,
      final Version... supportedVersions) {
    this(destinationConnectionId, sourceConnectionId, List.of(supportedVersions));
  }

  public VersionNegotiationPacket(
      final Optional<ConnectionId> destinationConnectionId,
      final Optional<ConnectionId> sourceConnectionId,
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

    ConnectionId.write(destinationConnectionId, sourceConnectionId, bb);

    for (final Version version : supportedVersions) {
      version.write(bb);
    }
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return sourceConnectionId;
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
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
