package com.protocol7.nettyquic.protocol;

import static com.protocol7.nettyquic.utils.Hex.dehex;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public enum Version {
  VERSION_NEGOTIATION(dehex("00000000")),
  FINAL(dehex("00000001")),
  QUIC_GO(dehex("51474fff")),
  DRAFT_15(dehex("ff00000f")),
  DRAFT_17(dehex("ff000011")),
  UNKNOWN(new byte[0]);

  public static final Version CURRENT = Version.QUIC_GO;

  public static Version read(final ByteBuf bb) {
    byte[] l = new byte[4];
    bb.readBytes(l);

    if (Arrays.equals(l, VERSION_NEGOTIATION.version)) {
      return VERSION_NEGOTIATION;
    } else if (Arrays.equals(l, FINAL.version)) {
      return FINAL;
    } else if (Arrays.equals(l, QUIC_GO.version)) {
      return QUIC_GO;
    } else if (Arrays.equals(l, DRAFT_15.version)) {
      return DRAFT_15;
    } else if (Arrays.equals(l, DRAFT_17.version)) {
      return DRAFT_17;
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
