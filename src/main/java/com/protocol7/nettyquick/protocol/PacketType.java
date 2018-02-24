package com.protocol7.nettyquick.protocol;

public enum PacketType {

  Initial((byte)0x7F),
  Retry((byte)0x7E),
  Handshake((byte)0x7D),
  Zero_RTT_Protected((byte)0x7C),
  One_octet((byte)0x1F),
  Two_octets((byte)0x1E),
  Four_octets((byte)0x1D);

  public static PacketType read(byte b) {

    if (b == Initial.type) {
      return Initial;
    } else if (b == Retry.type) {
      return Retry;
    } else if (b == Handshake.type) {
      return Handshake;
    } else if (b == Zero_RTT_Protected.type) {
      return Zero_RTT_Protected;
    } else if (b == One_octet.type) {
      return One_octet;
    } else if (b == Two_octets.type) {
      return Two_octets;
    } else if (b == Four_octets.type) {
      return Four_octets;
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
