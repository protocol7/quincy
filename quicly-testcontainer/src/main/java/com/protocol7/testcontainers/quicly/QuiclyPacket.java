package com.protocol7.testcontainers.quicly;

import com.protocol7.nettyquic.utils.Hex;

public class QuiclyPacket {

  private final boolean inbound;
  private final byte[] bytes;

  public QuiclyPacket(final boolean inbound, final byte[] bytes) {
    this.inbound = inbound;
    this.bytes = bytes;
  }

  public boolean isInbound() {
    return inbound;
  }

  public byte[] getBytes() {
    return bytes;
  }

  @Override
  public String toString() {
    return "QuiclyPacket{" + "inbound=" + inbound + ", bytes=" + Hex.hex(bytes) + '}';
  }
}
