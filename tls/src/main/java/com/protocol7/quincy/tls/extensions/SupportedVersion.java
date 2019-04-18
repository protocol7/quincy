package com.protocol7.quincy.tls.extensions;

import java.util.Arrays;

public enum SupportedVersion {
  TLS13(new byte[] {3, 4}),
  UNKNOWN(new byte[0]);

  public static SupportedVersion fromValue(final byte[] value) {
    if (value.length != 2) {
      throw new IllegalArgumentException("Invalid value length: " + value.length);
    }

    if (Arrays.equals(TLS13.value, value)) {
      return TLS13;
    } else {
      return UNKNOWN;
    }
  }

  private final byte[] value;

  SupportedVersion(final byte[] value) {
    this.value = value;
  }

  public byte[] getValue() {
    return value;
  }
}
