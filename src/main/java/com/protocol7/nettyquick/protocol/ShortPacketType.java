package com.protocol7.nettyquick.protocol;

public enum ShortPacketType {
  One_octet((byte)0x1F),
  Two_octets((byte)0x1E),
  Four_octets((byte)0x1D);

  private final byte type;

  ShortPacketType(final byte type) {
    this.type = type;
  }
}
