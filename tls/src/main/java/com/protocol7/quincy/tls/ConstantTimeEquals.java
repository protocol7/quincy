package com.protocol7.quincy.tls;

import java.security.MessageDigest;

public class ConstantTimeEquals {

  public static boolean isEqual(final byte[] a, final byte[] b) {
    return MessageDigest.isEqual(a, b);
  }
}
