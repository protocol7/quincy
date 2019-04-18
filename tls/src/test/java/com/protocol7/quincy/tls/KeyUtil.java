package com.protocol7.quincy.tls;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

public class KeyUtil {

  public static PublicKey getPublicKey(final String path) {
    try {
      final CertificateFactory fact = CertificateFactory.getInstance("X.509");

      final Certificate cert = fact.generateCertificate(new FileInputStream(path));
      return cert.getPublicKey();
    } catch (final GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static X509Certificate getCertFromCrt(final String path) {
    try {
      final FileInputStream fin = new FileInputStream(path);
      final CertificateFactory f = CertificateFactory.getInstance("X.509");
      return (X509Certificate) f.generateCertificate(fin);
    } catch (final GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }

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
