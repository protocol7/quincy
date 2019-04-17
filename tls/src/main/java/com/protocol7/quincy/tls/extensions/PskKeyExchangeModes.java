package com.protocol7.quincy.tls.extensions;

import static java.util.Arrays.asList;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PskKeyExchangeModes implements Extension {

  public static PskKeyExchangeModes defaults() {
    return new PskKeyExchangeModes(0x01); // PSK with (EC)DHE key establishment
  }

  public static PskKeyExchangeModes parse(ByteBuf bb) {
    bb.readByte(); // length

    List<Integer> exchangeModes = new ArrayList<>();
    while (bb.isReadable()) {
      exchangeModes.add((int) bb.readByte());
    }

    return new PskKeyExchangeModes(exchangeModes);
  }

  private final List<Integer> exchangeModes;

  public PskKeyExchangeModes(final List<Integer> exchangeModes) {
    this.exchangeModes = exchangeModes;
  }

  public PskKeyExchangeModes(final Integer... exchangeModes) {
    this(asList(exchangeModes));
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.PSK_KEY_EXCHANGE_MODES;
  }

  public List<Integer> getExchangeModes() {
    return exchangeModes;
  }

  @Override
  public void write(ByteBuf bb, boolean ignored) {
    bb.writeByte(exchangeModes.size());

    for (int exchangeMode : exchangeModes) {
      bb.writeByte(exchangeMode);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final PskKeyExchangeModes that = (PskKeyExchangeModes) o;
    return Objects.equals(exchangeModes, that.exchangeModes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exchangeModes);
  }

  @Override
  public String toString() {
    return "PskKeyExchangeModes{" + "exchangeModes=" + exchangeModes + '}';
  }
}
