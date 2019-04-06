package com.protocol7.nettyquic.tls;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.util.EnumSet;
import java.util.Optional;

import static com.protocol7.nettyquic.utils.Hex.dehex;
import static java.util.Optional.of;

public enum Group {
  // https://tools.ietf.org/html/rfc8446#appendix-B.3.1.4
  X25519(
      0x001d,
      "XDH",
      "XDH",
      new NamedParameterSpec("X25519"),
      dehex("302c300706032b656e0500032100"),
      dehex("302e020100300706032b656e05000420")),
  X448(
      0x001e,
      "XDH",
      "XDH",
      new NamedParameterSpec("X448"),
      dehex("3044300706032b656f0500033900"),
      dehex("3046020100300706032b656f05000438")),
  SECP256r1(
          0x0017,
      "EC",
      "ECDH",
      new ECGenParameterSpec("secp256r1"),
      //dehex("3059301306072a8648ce3d020106082a8648ce3d03010703420004"),
      dehex("3059301306072a8648ce3d020106"),
      //dehex("3041020100301306072a8648ce3d020106082a8648ce3d030107042730250201010420"));
      dehex("3041020100301306072a8648ce3d0201"));

  public static Optional<Group> fromValue(int value) {
    for(Group group : ALL) {
      if (group.value == value) {
        return of(group);
      }
    }

    return Optional.empty();
  }

  public static final EnumSet<Group> ALL = EnumSet.allOf(Group.class);

  private final int value;
  private final String keyPairGeneratorAlgo;
  private final String keyAgreementAlgo;
  private final AlgorithmParameterSpec parameterSpec;
  private final byte[] pkcsPublicPrefix;
  private final byte[] pkcsPrivatePrefix;

  Group(
      int value,
      final String keyPairGeneratorAlgo,
      final String keyAgreementAlgo,
      final AlgorithmParameterSpec parameterSpec,
      final byte[] pkcsPublicPrefix,
      final byte[] pkcsPrivatePrefix) {
    this.value = value;
    this.keyPairGeneratorAlgo = keyPairGeneratorAlgo;
    this.keyAgreementAlgo = keyAgreementAlgo;
    this.parameterSpec = parameterSpec;
    this.pkcsPublicPrefix = pkcsPublicPrefix;
    this.pkcsPrivatePrefix = pkcsPrivatePrefix;
  }

  public int getValue() {
    return value;
  }

  public String getKeyPairGeneratorAlgo() {
    return keyPairGeneratorAlgo;
  }

  public String getKeyAgreementAlgo() {
    return keyAgreementAlgo;
  }

  public AlgorithmParameterSpec getParameterSpec() {
    return parameterSpec;
  }

  public byte[] getPkcsPublicPrefix() {
    return pkcsPublicPrefix;
  }

  public byte[] getPkcsPrivatePrefix() {
    return pkcsPrivatePrefix;
  }
}
