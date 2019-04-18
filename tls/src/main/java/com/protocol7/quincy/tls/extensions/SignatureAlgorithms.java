package com.protocol7.quincy.tls.extensions;

import static java.util.Arrays.asList;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SignatureAlgorithms implements Extension {

  public static SignatureAlgorithms defaults() {
    return new SignatureAlgorithms(0x0804); // RSA-PSS-RSAE-SHA256
  }

  public static SignatureAlgorithms parse(final ByteBuf bb) {
    bb.readShort(); // length

    final List<Integer> algorithms = new ArrayList<>();
    while (bb.isReadable()) {
      algorithms.add((int) bb.readShort());
    }

    return new SignatureAlgorithms(algorithms);
  }

  private final List<Integer> algorithms;

  public SignatureAlgorithms(final List<Integer> algorithms) {
    this.algorithms = algorithms;
  }

  public SignatureAlgorithms(final Integer... algorithms) {
    this(asList(algorithms));
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.SIGNATURE_ALGORITHMS;
  }

  public List<Integer> getAlgorithms() {
    return algorithms;
  }

  @Override
  public void write(final ByteBuf bb, final boolean ignored) {
    bb.writeShort(algorithms.size() * 2);

    for (final int algorithm : algorithms) {
      bb.writeShort(algorithm);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final SignatureAlgorithms that = (SignatureAlgorithms) o;
    return Objects.equals(algorithms, that.algorithms);
  }

  @Override
  public int hashCode() {
    return Objects.hash(algorithms);
  }

  @Override
  public String toString() {
    return "SignatureAlgorithms{" + algorithms + '}';
  }
}
