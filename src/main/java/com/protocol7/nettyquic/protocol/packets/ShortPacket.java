package com.protocol7.nettyquic.protocol.packets;

import static com.protocol7.nettyquic.tls.EncryptionLevel.OneRtt;

import com.protocol7.nettyquic.protocol.*;
import com.protocol7.nettyquic.protocol.frames.Frame;
import com.protocol7.nettyquic.tls.aead.AEAD;
import com.protocol7.nettyquic.tls.aead.AEADProvider;
import io.netty.buffer.ByteBuf;
import java.util.Optional;

public class ShortPacket implements FullPacket {

  public static HalfParsedPacket<ShortPacket> parse(ByteBuf bb, int connIdLength) {
    bb.markReaderIndex();

    byte firstByte = bb.readByte();

    boolean keyPhase = (firstByte & 0x40) == 0x40;

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
        PacketNumber packetNumber = PacketNumber.parseVarint(bb);

        byte[] aad = new byte[bb.readerIndex()];
        bb.resetReaderIndex();
        bb.readBytes(aad);

        Payload payload =
            Payload.parse(bb, bb.readableBytes(), aeadProvider.get(OneRtt), packetNumber, aad);

        return new ShortPacket(keyPhase, connId, packetNumber, payload);
      }
    };
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
    byte b = 0;
    if (keyPhase) {
      b = (byte) (b | 0x40);
    }
    b = (byte) (b | 0x20); // constant
    b = (byte) (b | 0x10); // constant
    bb.writeByte(b);

    connectionId.get().write(bb);

    bb.writeBytes(packetNumber.write());

    byte[] aad = new byte[bb.writerIndex()];
    bb.markReaderIndex();
    bb.readBytes(aad);
    bb.resetReaderIndex();

    payload.write(bb, aead, packetNumber, aad);
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
