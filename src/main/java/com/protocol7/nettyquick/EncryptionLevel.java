package com.protocol7.nettyquick;

import com.protocol7.nettyquick.protocol.PacketType;
import com.protocol7.nettyquick.protocol.packets.HandshakePacket;
import com.protocol7.nettyquick.protocol.packets.InitialPacket;
import com.protocol7.nettyquick.protocol.packets.Packet;

public enum EncryptionLevel {
  Initial,
  Handshake,
  OneRtt;

  public static EncryptionLevel forPacket(Packet packet) {
    if (packet instanceof InitialPacket) {
      return Initial;
    } else if (packet instanceof HandshakePacket) {
      return Handshake;
    } else {
      return OneRtt;
    }
  }

  public static EncryptionLevel forPacketType(PacketType packetType) {
    if (packetType == PacketType.Initial) {
      return Initial;
    } else if (packetType == PacketType.Handshake) {
      return Handshake;
    } else {
      return OneRtt;
    }
  }
}
