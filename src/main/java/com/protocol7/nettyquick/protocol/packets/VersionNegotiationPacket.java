package com.protocol7.nettyquick.protocol.packets;

import com.google.common.collect.Lists;
import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;

public class VersionNegotiationPacket implements Packet {

  public static VersionNegotiationPacket parse(ByteBuf bb) {
    bb.readByte(); // TODO verify marker
    Version.read(bb); // TODO verify

    int cil = bb.readByte() & 0xFF;
    int dcil = ConnectionId.firstLength(cil);
    int scil = ConnectionId.lastLength(cil);

    Optional<ConnectionId> destConnId = ConnectionId.readOptional(dcil, bb);
    Optional<ConnectionId> srcConnId = ConnectionId.readOptional(scil, bb);

    List<Version> supported = Lists.newArrayList();
    while (bb.isReadable()) {
      try {
        supported.add(Version.read(bb));
      } catch (IllegalArgumentException e) {
        // ignore unknown versions
      }
    }
    return new VersionNegotiationPacket(destConnId, srcConnId, supported);
  }

  private final Optional<ConnectionId> destinationConnectionId;
  private final Optional<ConnectionId> sourceConnectionId;
  private final List<Version> supportedVersions;

  public VersionNegotiationPacket(
      Optional<ConnectionId> destinationConnectionId,
      Optional<ConnectionId> sourceConnectionId,
      Version... supportedVersions) {
    this(destinationConnectionId, sourceConnectionId, Lists.newArrayList(supportedVersions));
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
  public void write(ByteBuf bb, AEAD aead) {
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
}
