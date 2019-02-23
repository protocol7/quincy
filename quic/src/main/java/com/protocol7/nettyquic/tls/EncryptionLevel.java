package com.protocol7.nettyquic.tls;

import com.protocol7.nettyquic.protocol.packets.*;

public enum EncryptionLevel {
  Initial,
  Handshake,
  OneRtt;

  public static EncryptionLevel forPacket(Packet packet) {
    if (packet instanceof InitialPacket
        || packet instanceof RetryPacket
        || packet instanceof VersionNegotiationPacket) {
      return Initial;
    } else if (packet instanceof HandshakePacket) {
      return Handshake;
    } else {
      return OneRtt;
    }
  }
}
