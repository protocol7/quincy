package com.protocol7.nettyquick.protocol;

import io.netty.buffer.ByteBuf;

public enum Version {

  VERSION_NEGOTIATION(0x00000000),
  FINAL(0x00000001),
  DRAFT_15(0xff000000 + 15);

  public static final Version CURRENT = Version.DRAFT_15;

  public static Version read(final ByteBuf bb) {
    long l = bb.readInt();

    if (l == VERSION_NEGOTIATION.version) {
      return VERSION_NEGOTIATION;
    } else if (l == FINAL.version) {
      return FINAL;
    } else if (l == DRAFT_15.version) {
      return DRAFT_15;
    } else {
      throw new RuntimeException("Unknown version: " + l);
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
