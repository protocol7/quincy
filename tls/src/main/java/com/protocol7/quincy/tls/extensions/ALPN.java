package com.protocol7.quincy.tls.extensions;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ALPN implements Extension {

  public static ALPN parse(final ByteBuf bb) {
    final int len = bb.readShort(); // extension size

    final ByteBuf b = bb.readBytes(len);
    final List<String> protocols = new ArrayList<>();
    while (b.isReadable()) {
      final int protoLen = b.readByte();
      final byte[] bytes = new byte[protoLen];
      b.readBytes(bytes);
      protocols.add(new String(bytes, StandardCharsets.US_ASCII));
    }

    return new ALPN(protocols);
  }

  private final List<String> protocols;

  public ALPN(final List<String> protocols) {
    this.protocols = protocols;
  }

  public ALPN(final String... protocols) {
    this(List.of(protocols));
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION;
  }

  @Override
  public void write(final ByteBuf bb, final boolean ignored) {
    final int lenPos = bb.writerIndex();
    bb.writeShort(0); // overwrite below

    for (final String protocol : protocols) {
      final byte[] b = protocol.getBytes(StandardCharsets.US_ASCII);
      bb.writeByte(b.length);
      bb.writeBytes(b);
    }

    bb.setShort(lenPos, bb.writerIndex() - lenPos - 2);
  }

  public List<String> getProtocols() {
    return protocols;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final ALPN alpn = (ALPN) o;
    return Objects.equals(protocols, alpn.protocols);
  }

  @Override
  public int hashCode() {
    return Objects.hash(protocols);
  }

  @Override
  public String toString() {
    return "ALPN{" + protocols + '}';
  }
}
