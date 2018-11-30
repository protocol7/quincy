package com.protocol7.nettyquick.protocol.packets;

import static com.protocol7.nettyquick.EncryptionLevel.OneRtt;

import com.protocol7.nettyquick.protocol.*;
import com.protocol7.nettyquick.protocol.frames.Frame;
import com.protocol7.nettyquick.tls.aead.AEAD;
import com.protocol7.nettyquick.tls.aead.AEADProvider;
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

        return new ShortPacket(new ShortHeader(keyPhase, connId, packetNumber, payload));
      }
    };
  }

  private final ShortHeader header;

  public ShortPacket(ShortHeader header) {
    this.header = header;
  }

  @Override
  public PacketType getType() {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Packet addFrame(Frame frame) {
    return new ShortPacket(ShortHeader.addFrame(header, frame));
  }

  @Override
  public void write(ByteBuf bb, AEAD aead) {
    header.write(bb, aead);
  }

  @Override
  public PacketNumber getPacketNumber() {
    return header.getPacketNumber();
  }

  @Override
  public Optional<ConnectionId> getSourceConnectionId() {
    return Optional.empty();
  }

  @Override
  public Optional<ConnectionId> getDestinationConnectionId() {
    return header.getDestinationConnectionId();
  }

  @Override
  public Payload getPayload() {
    return header.getPayload();
  }

  @Override
  public String toString() {
    return "ShortPacket{" + "header=" + header + '}';
  }
}
