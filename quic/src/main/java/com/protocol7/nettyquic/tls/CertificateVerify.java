package com.protocol7.nettyquic.tls;

import com.protocol7.nettyquic.utils.Bytes;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;

public class CertificateVerify {

  private static final byte[] PADDING = new byte[64];

  static {
    Arrays.fill(PADDING, (byte) 32);
  }

  private static final byte[] SERVER_CONTEXT =
      "TLS 1.3, server CertificateVerify".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] CLIENT_CONTEXT =
      "TLS 1.3, client CertificateVerify".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] SEPARATOR = new byte[1];
  private static final byte[] SERVER_PREFIX = Bytes.concat(PADDING, SERVER_CONTEXT, SEPARATOR);
  private static final byte[] CLIENT_PREFIX = Bytes.concat(PADDING, CLIENT_CONTEXT, SEPARATOR);

  private static final String ALGORITHM = "RSASSA-PSS";
  private static final PSSParameterSpec PARAMETER_SPEC =
      new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);

  public static byte[] sign(byte[] data, PrivateKey key, boolean isClient) {
    byte[] prefix = isClient ? CLIENT_PREFIX : SERVER_PREFIX;
    byte[] toSign = Bytes.concat(prefix, data);

    try {
      Signature sig = Signature.getInstance(ALGORITHM);
      sig.setParameter(PARAMETER_SPEC);

      sig.initSign(key);
      sig.update(toSign);
      return sig.sign();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean verify(
      byte[] signature, byte[] data, PublicKey publicKey, boolean isClient) {
    byte[] prefix = isClient ? CLIENT_PREFIX : SERVER_PREFIX;
    byte[] toVerify = Bytes.concat(prefix, data);

    try {
      Signature sig = Signature.getInstance(ALGORITHM);
      sig.setParameter(PARAMETER_SPEC);

      sig.initVerify(publicKey);
      sig.update(toVerify);
      return sig.verify(signature);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }
}
