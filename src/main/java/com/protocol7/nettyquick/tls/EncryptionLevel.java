package com.protocol7.nettyquick.tls;

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
}
