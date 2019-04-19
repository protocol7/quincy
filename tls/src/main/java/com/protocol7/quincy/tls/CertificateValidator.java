package com.protocol7.quincy.tls;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

public interface CertificateValidator {

  static CertificateValidator defaults() {
    String trustStorePath = System.getProperty("javax.net.ssl.trustStore");
    String trustStorePwd = System.getProperty("javax.net.ssl.trustStorePassword");
    if (trustStorePath == null) {
      trustStorePath = System.getProperty("java.home") + "/lib/security/cacerts";
      trustStorePwd = "changeit";
    }

    final char[] pwd;
    if (trustStorePwd != null) {
      pwd = trustStorePwd.toCharArray();
    } else {
      pwd = null;
    }

    try {
      return new DefaultCertificateValidator(KeyStore.getInstance(new File(trustStorePath), pwd));
    } catch (final KeyStoreException
        | NoSuchAlgorithmException
        | CertificateException
        | IOException e) {
      throw new RuntimeException(e);
    }
  }

  boolean validate(List<byte[]> certificates);
}
