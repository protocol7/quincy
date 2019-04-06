package com.protocol7.nettyquick.it;

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

  public static List<byte[]> getCertsFromCrt(String path) {
    try {
      FileInputStream fin = new FileInputStream(path);
      CertificateFactory f = CertificateFactory.getInstance("X.509");
      Certificate cert = f.generateCertificate(fin);
      return List.of(cert.getEncoded());
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static PrivateKey getPrivateKey(String path) {
    try {
      byte[] b = Files.readAllBytes(Path.of(path));
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(b);

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");

      return keyFactory.generatePrivate(keySpec);
    } catch (GeneralSecurityException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
