package com.protocol7.quincy.tls;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import org.junit.Test;

public class DefaultCertificateValidatorTest {

  @Test
  public void google() throws IOException {
    final CertificateValidator validator = CertificateValidator.defaults();

    final byte[] cert1 = Files.readAllBytes(new File("src/test/resources/google1.crt").toPath());
    final byte[] cert2 = Files.readAllBytes(new File("src/test/resources/google2.crt").toPath());

    assertTrue(validator.validate(List.of(cert1, cert2)));
  }

  @Test
  public void googleMissingIntermediate() throws IOException {
    final CertificateValidator validator = CertificateValidator.defaults();

    final byte[] cert1 = Files.readAllBytes(new File("src/test/resources/google1.crt").toPath());

    assertFalse(validator.validate(List.of(cert1)));
  }

  @Test
  public void selfSigned() throws GeneralSecurityException, IOException {
    final KeyStore truststore = KeyStore.getInstance("JKS");
    final X509Certificate cert = KeyUtil.getCertFromCrt("src/test/resources/server.crt");
    truststore.load(() -> new KeyStore.PasswordProtection("hello".toCharArray()));
    truststore.setCertificateEntry("pwn", cert);

    final CertificateValidator validator = new DefaultCertificateValidator(truststore);

    final byte[] certBytes = Files.readAllBytes(new File("src/test/resources/server.crt").toPath());

    assertTrue(validator.validate(List.of(certBytes)));
  }

  @Test
  public void selfSignedNotTrusted() throws IOException {
    final CertificateValidator validator = CertificateValidator.defaults();

    final byte[] certBytes = Files.readAllBytes(new File("src/test/resources/server.crt").toPath());

    assertFalse(validator.validate(List.of(certBytes)));
  }
}
