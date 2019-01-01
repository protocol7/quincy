package com.protocol7.nettyquic.protocol;

import io.netty.buffer.ByteBuf;

public enum Version {
  VERSION_NEGOTIATION(0x00000000),
  FINAL(0x00000001),
  QUIC_GO(0x00000065),
  DRAFT_15(0xff000000 + 15);

  public static final Version CURRENT = Version.QUIC_GO;

  public static Version read(final ByteBuf bb) {
    long l = bb.readInt();

    if (l == VERSION_NEGOTIATION.version) {
      return VERSION_NEGOTIATION;
    } else if (l == FINAL.version) {
      return FINAL;
    } else if (l == QUIC_GO.version) {
      return QUIC_GO;
    } else if (l == DRAFT_15.version) {
      return DRAFT_15;
    } else {
      throw new IllegalArgumentException("Unknown version: " + l);
    }
  }

  private final int version;

  Version(final int version) {
    this.version = version;
  }

  public void write(final ByteBuf bb) {
    bb.writeInt(version);
  }
}
