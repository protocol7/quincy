package com.protocol7.nettyquic.tls;

import com.protocol7.nettyquic.protocol.packets.HandshakePacket;
import com.protocol7.nettyquic.protocol.packets.InitialPacket;
import com.protocol7.nettyquic.protocol.packets.Packet;

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
