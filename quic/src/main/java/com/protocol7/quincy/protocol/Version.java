package com.protocol7.quincy.protocol;

import static com.protocol7.quincy.utils.Hex.dehex;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public enum Version {
  VERSION_NEGOTIATION(dehex("00000000")),
  FINAL(dehex("00000001")),
  DRAFT_29(dehex("ff00001d")),
  UNKNOWN(new byte[0]);

  public static Version read(final ByteBuf bb) {
    final byte[] l = new byte[4];
    bb.readBytes(l);

    if (Arrays.equals(l, VERSION_NEGOTIATION.version)) {
      return VERSION_NEGOTIATION;
    } else if (Arrays.equals(l, FINAL.version)) {
      return FINAL;
    } else if (Arrays.equals(l, DRAFT_29.version)) {
      return DRAFT_29;
    } else {
      return UNKNOWN;
    }
  }

  private final byte[] version;

  Version(final byte[] version) {
    this.version = version;
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(version);
  }

  public byte[] asBytes() {
    return version;
  }
}
