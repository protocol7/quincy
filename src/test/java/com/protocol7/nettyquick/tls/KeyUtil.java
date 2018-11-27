package com.protocol7.nettyquick.tls;

import com.google.common.collect.ImmutableList;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

public class KeyUtil {

  public static PublicKey getPublicKeyFromPem(String path) {
    try {
      PemReader reader = new PemReader(new FileReader(path));
      PemObject pemObject = reader.readPemObject();
      byte[] content = pemObject.getContent();
      CertificateFactory fact = CertificateFactory.getInstance("X.509");

      Certificate cert = fact.generateCertificate(new ByteArrayInputStream(content));
      return cert.getPublicKey();
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static X509Certificate getCertFromCrt(String path) {
    try {
      FileInputStream fin = new FileInputStream(path);
      CertificateFactory f = CertificateFactory.getInstance("X.509");
      return (X509Certificate) f.generateCertificate(fin);
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<byte[]> getCertsFromCrt(String path) {
    try {
      FileInputStream fin = new FileInputStream(path);
      CertificateFactory f = CertificateFactory.getInstance("X.509");
      Certificate cert = f.generateCertificate(fin);
      return ImmutableList.of(cert.getEncoded());
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static RSAPrivateKey getPrivateKeyFromPem(String path) {
    try {
      PemReader reader = new PemReader(new FileReader(path));
      PemObject pemObject = reader.readPemObject();
      byte[] content = pemObject.getContent();
      PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(content);
      KeyFactory factory = KeyFactory.getInstance("RSA");
      return (RSAPrivateKey) factory.generatePrivate(privKeySpec);
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
