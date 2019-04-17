package com.protocol7.quincy.tls.extensions;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SupportedVersions implements Extension {

  public static final SupportedVersions TLS13 = new SupportedVersions(SupportedVersion.TLS13);

  public static SupportedVersions parse(ByteBuf bb, boolean isClient) {
    int len = 2;
    if (!isClient) {
      len = bb.readByte();
    }
    if (len % 2 != 0) {
      throw new IllegalArgumentException("Invalid version length, must be divisible by 2: " + len);
    }
    len /= 2;

    List<SupportedVersion> versions = new ArrayList<>(len);
    byte[] b = new byte[2];
    for (int i = 0; i < len; i++) {
      bb.readBytes(b);
      versions.add(SupportedVersion.fromValue(b));
    }

    return new SupportedVersions(versions);
  }

  private final List<SupportedVersion> versions;

  private SupportedVersions(List<SupportedVersion> versions) {
    this.versions = versions;
  }

  private SupportedVersions(SupportedVersion... versions) {
    this.versions = Arrays.asList(versions);
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.SUPPORTED_VERSIONS;
  }

  public List<SupportedVersion> getVersions() {
    return versions;
  }

  @Override
  public void write(ByteBuf bb, boolean isClient) {
    if (isClient) {
      bb.writeByte(versions.size() * 2);
    }
    for (SupportedVersion version : versions) {
      bb.writeBytes(version.getValue());
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SupportedVersions that = (SupportedVersions) o;
    return Objects.equals(versions, that.versions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(versions);
  }

  @Override
  public String toString() {
    return "SupportedVersions{" + "versions=" + versions + '}';
  }
}
