package com.protocol7.nettyquic.tls;

import com.protocol7.nettyquic.utils.Bytes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

  private static final ThreadLocal<MessageDigest> digests = ThreadLocal.withInitial(() -> {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  });

  public static byte[] sha256(byte[]... data) {
    return digests.get().digest(Bytes.concat(data));
  }
}
