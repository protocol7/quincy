package com.protocol7.quincy.tls.extensions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ALPN implements Extension {

  public static ALPN parse(final ByteBuf bb) {
    final int len = bb.readShort(); // extension size

    final ByteBuf buf = bb.readBytes(len);
    final List<String> protocols = new ArrayList<>();

    while (buf.isReadable()) {
      final int l = buf.readByte();
      final byte[] b = new byte[l];

      buf.readBytes(b);

      protocols.add(new String(b, StandardCharsets.UTF_8));
    }

    return new ALPN(protocols);
  }

  public static byte[] from(final String... protocols) {
    final ByteBuf bb = Unpooled.buffer();
    try {
      for (final String protocol : protocols) {
        final byte[] b = protocol.getBytes(StandardCharsets.US_ASCII);
        bb.writeByte(b.length);
        bb.writeBytes(b);
      }

      final byte[] b = new byte[bb.readableBytes()];
      bb.readBytes(b);
      return b;
    } finally {
      bb.release();
    }
  }

  public static byte[] from(final byte[]... protocols) {
    final ByteBuf bb = Unpooled.buffer();
    try {
      for (final byte[] protocol : protocols) {
        bb.writeByte(protocol.length);
        bb.writeBytes(protocol);
      }

      final byte[] b = new byte[bb.readableBytes()];
      bb.readBytes(b);
      return b;
    } finally {
      bb.release();
    }
  }

  private final List<String> protocols;

  public ALPN(final String... protocols) {
    this.protocols = Arrays.asList(protocols);
  }

  public ALPN(final List<String> protocols) {
    this.protocols = protocols;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION;
  }

  @Override
  public void write(final ByteBuf bb, final boolean ignored) {
    final ByteBuf buf = Unpooled.buffer();
    for (final String protocol : protocols) {
      final byte[] b = protocol.getBytes(StandardCharsets.UTF_8);

      buf.writeByte(b.length);
      buf.writeBytes(b);
    }

    bb.writeShort(buf.writerIndex());
    bb.writeBytes(buf);
  }

  public List<String> getProtocols() {
    return protocols;
  }

  public boolean contains(final String protocol) {
    return protocols.contains(protocol);
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
