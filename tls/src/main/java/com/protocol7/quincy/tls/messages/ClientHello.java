package com.protocol7.quincy.tls.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.protocol7.quincy.tls.CipherSuite;
import com.protocol7.quincy.tls.Group;
import com.protocol7.quincy.tls.KeyExchange;
import com.protocol7.quincy.tls.extensions.Extension;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
import com.protocol7.quincy.tls.extensions.PskKeyExchangeModes;
import com.protocol7.quincy.tls.extensions.SignatureAlgorithms;
import com.protocol7.quincy.tls.extensions.SupportedGroups;
import com.protocol7.quincy.tls.extensions.SupportedVersions;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Hex;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ClientHello {

  public static ClientHello defaults(final KeyExchange ke, final Extension... exts) {
    final byte[] clientRandom = Rnd.rndBytes(32);
    final byte[] sessionId = new byte[0];
    final List<CipherSuite> cipherSuites = CipherSuite.SUPPORTED;
    final List<Extension> extensions =
        ImmutableList.<Extension>builder()
            .add(
                KeyShare.of(ke.getGroup(), ke.getPublicKey()),
                SignatureAlgorithms.defaults(),
                new SupportedGroups(Group.X25519),
                SupportedVersions.TLS13,
                PskKeyExchangeModes.defaults())
            .add(exts)
            .build();

    return new ClientHello(clientRandom, sessionId, cipherSuites, extensions);
  }

  public static ClientHello parse(final byte[] ch, final boolean isClient) {
    final ByteBuf bb = Unpooled.wrappedBuffer(ch);

    final byte handshakeType = bb.readByte();

    if (handshakeType != 0x01) {
      throw new IllegalArgumentException("Invalid handshake type");
    }

    final byte[] b = new byte[3];
    bb.readBytes(b);
    final int payloadLength = (b[0] & 0xFF) << 16 | (b[1] & 0xFF) << 8 | (b[2] & 0xFF);
    if (payloadLength != bb.readableBytes()) {
      throw new IllegalArgumentException(
          "Buffer incorrect length: actual " + payloadLength + ", expected " + bb.readableBytes());
    }

    final byte[] clientVersion = new byte[2];
    bb.readBytes(clientVersion);
    if (!Arrays.equals(clientVersion, new byte[] {3, 3})) {
      throw new IllegalArgumentException("Invalid client version: " + Hex.hex(clientVersion));
    }

    final byte[] clientRandom = new byte[32];
    bb.readBytes(clientRandom); // client random

    final int sessionIdLen = bb.readByte();
    final byte[] sessionId = new byte[sessionIdLen];
    bb.readBytes(sessionId); // session ID

    final List<CipherSuite> cipherSuites = CipherSuite.parseKnown(bb);

    final byte[] compression = new byte[2];
    bb.readBytes(compression);
    if (!Arrays.equals(compression, new byte[] {1, 0})) {
      throw new IllegalArgumentException("Compression must be disabled: " + Hex.hex(compression));
    }

    final int extensionsLen = bb.readShort();
    final ByteBuf extBB = bb.readBytes(extensionsLen);
    try {
      final List<Extension> ext = Extension.parseAll(extBB, isClient);

      return new ClientHello(clientRandom, sessionId, cipherSuites, ext);
    } finally {
      extBB.release();
    }
  }

  private final byte[] clientRandom;
  private final byte[] sessionId;
  private final List<CipherSuite> cipherSuites;
  private final List<Extension> extensions;

  public ClientHello(
      final byte[] clientRandom,
      final byte[] sessionId,
      final List<CipherSuite> cipherSuites,
      final List<Extension> extensions) {
    Preconditions.checkArgument(clientRandom.length == 32);
    this.clientRandom = clientRandom;
    this.sessionId = requireNonNull(sessionId);
    this.cipherSuites = requireNonNull(cipherSuites);
    this.extensions = extensions;
  }

  public byte[] getClientRandom() {
    return clientRandom;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public List<CipherSuite> getCipherSuites() {
    return cipherSuites;
  }

  public List<Extension> getExtensions() {
    return extensions;
  }

  public Optional<Extension> getExtension(final ExtensionType type) {
    for (final Extension ext : extensions) {
      if (ext.getType().equals(type)) {
        return Optional.of(ext);
      }
    }
    return Optional.empty();
  }

  public void write(final ByteBuf bb, final boolean isClient) {
    bb.writeByte(0x01);

    // payload length
    final int lenPos = bb.writerIndex();
    Bytes.write24(bb, 0); // placeholder

    // version
    bb.writeByte(0x03);
    bb.writeByte(0x03);

    bb.writeBytes(clientRandom);

    bb.writeByte(sessionId.length);
    bb.writeBytes(sessionId);

    CipherSuite.writeAll(bb, cipherSuites);

    // compression
    bb.writeByte(0x01);
    bb.writeByte(0x00);

    final int extLenPos = bb.writerIndex();
    bb.writeShort(0);
    Extension.writeAll(extensions, bb, isClient);

    bb.setShort(extLenPos, bb.writerIndex() - extLenPos - 2);

    Bytes.set24(bb, lenPos, bb.writerIndex() - lenPos - 3);
  }

  @Override
  public String toString() {
    return "ClientHello{"
        + "clientRandom="
        + Hex.hex(clientRandom)
        + ", sessionId="
        + Hex.hex(sessionId)
        + ", cipherSuites="
        + cipherSuites
        + ", extensions="
        + extensions
        + '}';
  }
}
