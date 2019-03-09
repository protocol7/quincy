package com.protocol7.nettyquic.tls.extensions;

import com.protocol7.nettyquic.utils.Hex;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;

public class SupportedVersions implements Extension {

  public static final SupportedVersions TLS13 = new SupportedVersions(new byte[] {3, 4});

  public static SupportedVersions parse(ByteBuf bb, boolean isClient) {
    int len = 2;
    if (!isClient) {
      len = bb.readByte();
    }

    byte[] version = new byte[len];
    bb.readBytes(version);

    return new SupportedVersions(version);
  }

  private final byte[] version;

  private SupportedVersions(byte[] version) {
    this.version = version;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.supported_versions;
  }

  public byte[] getVersion() {
    return version;
  }

  @Override
  public void write(ByteBuf bb, boolean isClient) {
    if (isClient) {
      bb.writeByte(version.length);
    }
    bb.writeBytes(version);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SupportedVersions that = (SupportedVersions) o;
    return Arrays.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(version);
  }

  @Override
  public String toString() {
    return "SupportedVersions{" + Hex.hex(version) + '}';
  }
}
