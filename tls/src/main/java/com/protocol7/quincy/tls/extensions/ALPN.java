package com.protocol7.quincy.tls.extensions;

import com.protocol7.quincy.utils.Hex;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ALPN implements Extension {

  public static ALPN parse(final ByteBuf bb) {
    final int len = bb.readShort(); // extension size

    final byte[] protocols = new byte[len];
    bb.readBytes(protocols);

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

  private final byte[] protocols;

  public ALPN(final byte[] protocols) {
    this.protocols = protocols;
  }

  @Override
  public ExtensionType getType() {
    return ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION;
  }

  @Override
  public void write(final ByteBuf bb, final boolean ignored) {
    bb.writeShort(protocols.length);
    bb.writeBytes(protocols);
  }

  public byte[] getProtocols() {
    return protocols;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final ALPN alpn = (ALPN) o;
    return Arrays.equals(protocols, alpn.protocols);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(protocols);
  }

  @Override
  public String toString() {
    return "ALPN{" + Hex.hex(protocols) + '}';
  }
}
