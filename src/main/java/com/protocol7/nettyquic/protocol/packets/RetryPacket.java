package com.protocol7.nettyquic.protocol.packets;

import com.protocol7.nettyquic.protocol.ConnectionId;
import com.protocol7.nettyquic.protocol.Version;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADProvider;
import com.protocol7.nettyquic.utils.Hex;
import com.protocol7.nettyquic.utils.Opt;
import io.netty.buffer.ByteBuf;
import java.util.Optional;

public class RetryPacket implements Packet {

  public static HalfParsedPacket<RetryPacket> parse(ByteBuf bb) {
    byte b = bb.readByte(); // TODO verify reserved and packet types

    int odcil = ConnectionId.lastLength(b & 0xFF);
    Version version = Version.read(bb);

    int cil = bb.readByte() & 0xFF;
    int dcil = ConnectionId.firstLength(cil);
    int scil = ConnectionId.lastLength(cil);

    Optional<ConnectionId> destConnId = ConnectionId.readOptional(dcil, bb);
    Optional<ConnectionId> srcConnId = ConnectionId.readOptional(scil, bb);

    ConnectionId orgConnId = ConnectionId.readOptional(odcil, bb).get();

    byte[] retryToken = new byte[bb.readableBytes()];
    bb.readBytes(retryToken);

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
      public RetryPacket complete(AEADProvider aeadProvider) {
        return new RetryPacket(version, destConnId, srcConnId, orgConnId, retryToken);
      }
    };
  }

  private final Version version;
  private final Optional<ConnectionId> destinationConnectionId;
  private final Optional<ConnectionId> sourceConnectionId;
  private final ConnectionId originalConnectionId;
  private final byte[] retryToken;

  public RetryPacket(
      Version version,
      Optional<ConnectionId> destinationConnectionId,
      Optional<ConnectionId> sourceConnectionId,
      ConnectionId originalConnectionId,
      byte[] retryToken) {
    this.version = version;
    this.destinationConnectionId = destinationConnectionId;
    this.sourceConnectionId = sourceConnectionId;
    this.originalConnectionId = originalConnectionId;
    this.retryToken = retryToken;
  }

  public Optional<ConnectionId> getDestinationConnectionId() {
    return destinationConnectionId;
  }

  @Override
  public void write(ByteBuf bb, AEAD aead) {
    int b = (PACKET_TYPE_MASK | PacketType.Retry.getType() << 4) & 0xFF;
    b = b | 0x40; // fixed

    b |= ((originalConnectionId.getLength() - 3) & 0b1111);
    bb.writeByte(b);

    version.write(bb);

    bb.writeByte(ConnectionId.joinLenghts(destinationConnectionId, sourceConnectionId));
    if (destinationConnectionId.isPresent()) {
      destinationConnectionId.get().write(bb);
    }
    if (sourceConnectionId.isPresent()) {
      sourceConnectionId.get().write(bb);
    }

    originalConnectionId.write(bb);

    bb.writeBytes(retryToken);
  }

  public Optional<ConnectionId> getSourceConnectionId() {
    return sourceConnectionId;
  }

  public ConnectionId getOriginalConnectionId() {
    return originalConnectionId;
  }

  public byte[] getRetryToken() {
    return retryToken;
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
