package com.protocol7.quincy.protocol;

import static com.protocol7.quincy.utils.Hex.dehex;
import static com.protocol7.quincy.utils.Hex.hex;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class Version {
  public static final Version VERSION_NEGOTIATION = new Version(dehex("00000000"));
  public static final Version FINAL = new Version(dehex("00000001"));
  public static final Version DRAFT_29 = new Version(dehex("ff00001d"));

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
      return new Version(l);
    }
  }

  private final byte[] version;

  public Version(final byte[] version) {
    this.version = version;
  }

  public void write(final ByteBuf bb) {
    bb.writeBytes(version);
  }

  public byte[] asBytes() {
    return version;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Version version1 = (Version) o;
    return Arrays.equals(version, version1.version);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(version);
  }

  @Override
  public String toString() {
    return "Version[" + hex(version) + ']';
  }
}
