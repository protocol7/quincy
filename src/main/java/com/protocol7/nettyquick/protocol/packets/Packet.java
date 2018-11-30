package com.protocol7.nettyquick.protocol.packets;

import com.protocol7.nettyquick.protocol.ConnectionId;
import com.protocol7.nettyquick.protocol.Version;
import com.protocol7.nettyquick.tls.aead.AEAD;
import io.netty.buffer.ByteBuf;
import java.util.Optional;

public interface Packet {

  int PACKET_TYPE_MASK = 0b10000000;

  static boolean isLongHeader(int b) {
    return (PACKET_TYPE_MASK & b) == PACKET_TYPE_MASK;
  }

  static HalfParsedPacket parse(ByteBuf bb, int connidLength) {
    bb.markReaderIndex();
    int firstByte = bb.readByte() & 0xFF;

    if (isLongHeader(firstByte)) {
      // Long header packet

      // might be a ver neg packet, so we must check the version
      Version version = Version.read(bb);
      bb.resetReaderIndex();

      if (version == Version.VERSION_NEGOTIATION) {
        return VersionNegotiationPacket.parse(bb);
      } else if (firstByte == InitialPacket.MARKER) {
        return InitialPacket.parse(bb);
      } else if (firstByte == HandshakePacket.MARKER) {
        return HandshakePacket.parse(bb);
      } else if (firstByte == RetryPacket.MARKER) {
        return RetryPacket.parse(bb);
      } else {
        throw new RuntimeException("Unknown long header packet");
      }
    } else {
      // short header packet
      bb.resetReaderIndex();
      return ShortPacket.parse(bb, connidLength);
    }
  }

  void write(ByteBuf bb, AEAD aead);

  Optional<ConnectionId> getSourceConnectionId();

  Optional<ConnectionId> getDestinationConnectionId();
}
