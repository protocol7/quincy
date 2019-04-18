package com.protocol7.quincy.it;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

public class KeyUtil {

  public static List<byte[]> getCertsFromCrt(final String path) {
    try {
      final FileInputStream fin = new FileInputStream(path);
      final CertificateFactory f = CertificateFactory.getInstance("X.509");
      final Certificate cert = f.generateCertificate(fin);
      return List.of(cert.getEncoded());
    } catch (final GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey getPrivateKey(final String path) {
    try {
      final byte[] b = Files.readAllBytes(Path.of(path));
      final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(b);

      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

      return keyFactory.generatePrivate(keySpec);
    } catch (final GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
