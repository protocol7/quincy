package com.protocol7.quincy.tls.extensions;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Extension {

  static List<Extension> parseAll(final ByteBuf bb, final boolean isClient) {
    final List<Extension> extensions = new ArrayList<>();

    while (bb.isReadable()) {
      final Extension ext = Extension.parse(bb, isClient);

      extensions.add(ext);
    }

    return extensions;
  }

  static Extension parse(final ByteBuf bb, final boolean isClient) {
    final ExtensionType type = ExtensionType.fromValue(bb.readShort() & 0xFFFF);

    final int len = bb.readShort();
    final ByteBuf b = bb.readBytes(len);
    try {
      if (type == ExtensionType.QUIC) {
        return TransportParameters.parse(b, isClient);
      } else if (type == ExtensionType.KEY_SHARE) {
        return KeyShare.parse(b, isClient);
      } else if (type == ExtensionType.SUPPORTED_VERSIONS) {
        return SupportedVersions.parse(b, isClient);
      } else if (type == ExtensionType.SUPPORTED_GROUPS) {
        return SupportedGroups.parse(b);
      } else if (type == ExtensionType.SIGNATURE_ALGORITHMS) {
        return SignatureAlgorithms.parse(b);
      } else if (type == ExtensionType.PSK_KEY_EXCHANGE_MODES) {
        return PskKeyExchangeModes.parse(b);
      } else if (type == ExtensionType.SERVER_NAME) {
        return ServerName.parse(b);
      } else {
        return RawExtension.parse(type, b);
      }
    } finally {
      b.release();
    }
  }

  static void writeAll(
      final Collection<Extension> extensions, final ByteBuf bb, final boolean isClient) {
    for (final Extension extension : extensions) {
      bb.writeShort(extension.getType().getValue());

      final int lenPos = bb.writerIndex();
      bb.writeShort(0);

      extension.write(bb, isClient);

      bb.setShort(lenPos, bb.writerIndex() - lenPos - 2);
    }
  }

  ExtensionType getType();

  void write(ByteBuf bb, boolean isClient);
}
