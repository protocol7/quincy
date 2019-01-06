package com.protocol7.nettyquic.protocol.packets;

public enum PacketType {
  Initial((byte) 0x0),
  Zero_RTT_Protected((byte) 0x01),
  Handshake((byte) 0x02),
  Retry((byte) 0x03);

  public static PacketType fromByte(byte b) {

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
