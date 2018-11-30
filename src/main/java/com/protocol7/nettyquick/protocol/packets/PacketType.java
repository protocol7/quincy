package com.protocol7.nettyquick.protocol.packets;

public enum PacketType {
  Initial((byte) 0x7F),
  Retry((byte) 0x7E),
  Handshake((byte) 0x7D),
  Zero_RTT_Protected((byte) 0x7C);

  public static PacketType read(byte b) {

    if (b == Initial.type) {
      return Initial;
    } else if (b == Retry.type) {
      return Retry;
    } else if (b == Handshake.type) {
      return Handshake;
    } else if (b == Zero_RTT_Protected.type) {
      return Zero_RTT_Protected;
    } else {
      throw new RuntimeException("Unknown long packet type");
    }
  }

  private final byte type;

  PacketType(final byte type) {
    this.type = type;
  }

  public byte getType() {
    return type;
  }
}
