package com.protocol7.nettyquic.protocol.packets;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADProvider;
import com.protocol7.nettyquic.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VersionNegotiationPacket implements Packet {

  public static HalfParsedPacket<VersionNegotiationPacket> parse(ByteBuf bb) {
    bb.readByte(); // TODO verify marker
    Version version = Version.read(bb);

    if (version != Version.VERSION_NEGOTIATION) {
      throw new IllegalArgumentException("Invalid version");
    }

    int cil = bb.readByte() & 0xFF;

    int dcil = ConnectionId.firstLength(cil);
    int scil = ConnectionId.lastLength(cil);

    Optional<ConnectionId> destConnId = ConnectionId.readOptional(dcil, bb);
    Optional<ConnectionId> srcConnId = ConnectionId.readOptional(scil, bb);

    List<Version> supported = new ArrayList<>();
    while (bb.isReadable()) {
      try {
        supported.add(Version.read(bb));
      } catch (IllegalArgumentException e) {
        // ignore unknown versions
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
      Optional<ConnectionId> destinationConnectionId,
      Optional<ConnectionId> sourceConnectionId,
      Version... supportedVersions) {
    this(destinationConnectionId, sourceConnectionId, List.of(supportedVersions));
  }

  public VersionNegotiationPacket(
      Optional<ConnectionId> destinationConnectionId,
      Optional<ConnectionId> sourceConnectionId,
      List<Version> supportedVersions) {
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;

    if (supportedVersions.isEmpty()) {
      throw new IllegalArgumentException("Supported versions must not be empty");
    }
    this.supportedVersions = supportedVersions;
  }

  @Override
  public void write(ByteBuf bb, AEAD notUsed) {
    int marker = Rnd.rndInt() & 0xFF;
    marker |= 0b10000000;
    bb.writeByte(marker);
    Version.VERSION_NEGOTIATION.write(bb);
    bb.writeByte(ConnectionId.joinLenghts(destinationConnectionId, sourceConnectionId));
    if (destinationConnectionId.isPresent()) {
      destinationConnectionId.get().write(bb);
    }
    if (sourceConnectionId.isPresent()) {
      sourceConnectionId.get().write(bb);
    }

    for (Version version : supportedVersions) {
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
