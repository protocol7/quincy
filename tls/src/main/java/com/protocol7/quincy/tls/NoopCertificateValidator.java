package com.protocol7.quincy.tls;

import java.util.List;

public class NoopCertificateValidator implements CertificateValidator {

  public static final CertificateValidator INSTANCE = new NoopCertificateValidator();

  private NoopCertificateValidator() {}

  @Override
  public boolean validate(final List<byte[]> certificates) {
    return true;
  }
}
