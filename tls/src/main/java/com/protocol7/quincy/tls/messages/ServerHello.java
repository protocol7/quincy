package com.protocol7.quincy.tls.messages;

import static com.protocol7.quincy.tls.messages.MessageType.SERVER_HELLO;
import static com.protocol7.quincy.utils.Hex.hex;

import com.google.common.collect.ImmutableList;
import com.protocol7.quincy.Writeable;
import com.protocol7.quincy.tls.CipherSuite;
import com.protocol7.quincy.tls.KeyExchange;
import com.protocol7.quincy.tls.extensions.Extension;
import com.protocol7.quincy.tls.extensions.ExtensionType;
import com.protocol7.quincy.tls.extensions.KeyShare;
import com.protocol7.quincy.tls.extensions.SupportedVersions;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ServerHello implements Message, Writeable {

  private static final MessageType TYPE = SERVER_HELLO;
  private static final byte[] VERSION = new byte[] {0x03, 0x03};

  public static ServerHello defaults(final KeyExchange ke, final Extension... exts) {
    final byte[] clientRandom = Rnd.rndBytes(32);
    final byte[] sessionId = new byte[0];
    final CipherSuite cipherSuites = CipherSuite.TLS_AES_128_GCM_SHA256;
    final List<Extension> extensions =
        ImmutableList.<Extension>builder()
            .add(KeyShare.of(ke.getGroup(), ke.getPublicKey()), SupportedVersions.TLS13)
            .add(exts)
            .build();

    return new ServerHello(clientRandom, sessionId, cipherSuites, extensions);
  }

  public static ServerHello parse(final ByteBuf bb, final boolean isClient) {
    final int messageType = bb.readByte(); // server hello
    if (messageType != TYPE.getType()) {
      throw new IllegalArgumentException("Not a server hello");
    }

    Bytes.read24(bb); // payloadLength

    final byte[] version = new byte[2];
    bb.readBytes(version);
    if (!Arrays.equals(version, VERSION)) {
      throw new IllegalArgumentException("Illegal version");
    }

    final byte[] serverRandom = new byte[32];
    bb.readBytes(serverRandom); // server random

    final int sessionIdLen = bb.readByte();
    final byte[] sessionId = new byte[sessionIdLen];

    final Optional<CipherSuite> cipherSuite = CipherSuite.parseOne(bb);
    // TODO implement all know cipher suites
    if (!cipherSuite.isPresent()) {
      throw new IllegalArgumentException("Illegal cipher suite");
    }

    bb.readByte(); // compressionMethod

    final int extensionLen = bb.readShort();
    final ByteBuf extBB = bb.readBytes(extensionLen);
    try {
      final List<Extension> extensions = Extension.parseAll(extBB, isClient);
      return new ServerHello(serverRandom, sessionId, cipherSuite.get(), extensions);
    } finally {
      extBB.release();
    }
  }

  public void write(final ByteBuf bb) {
    bb.writeByte(TYPE.getType());

    final int lenPosition = bb.writerIndex();
    // write placeholder
    bb.writeBytes(new byte[3]);
    bb.writeBytes(VERSION);

    bb.writeBytes(serverRandom);
    bb.writeByte(sessionId.length);
    bb.writeBytes(sessionId);
    bb.writeShort(cipherSuites.getValue());
    bb.writeByte(0);

    final int extPosition = bb.writerIndex();
    bb.writeShort(0); // placeholder

    Extension.writeAll(extensions, bb, false);
    bb.setShort(extPosition, bb.writerIndex() - extPosition - 2);

    // update length
    Bytes.set24(bb, lenPosition, bb.writerIndex() - lenPosition - 3);
  }

  private final byte[] serverRandom;
  private final byte[] sessionId;
  private final CipherSuite cipherSuites;
  private final List<Extension> extensions;

  public ServerHello(
      final byte[] serverRandom,
      final byte[] sessionId,
      final CipherSuite cipherSuites,
      final List<Extension> extensions) {
    this.serverRandom = serverRandom;
    this.sessionId = sessionId;
    this.cipherSuites = cipherSuites;
    this.extensions = extensions;
  }

  public byte[] getServerRandom() {
    return serverRandom;
  }

  public byte[] getSessionId() {
    return sessionId;
  }

  public CipherSuite getCipherSuites() {
    return cipherSuites;
  }

  public List<Extension> getExtensions() {
    return extensions;
  }

  public Optional<Extension> geExtension(final ExtensionType type) {
    for (final Extension ext : extensions) {
      if (ext.getType().equals(type)) {
        return Optional.of(ext);
      }
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "ServerHello{"
        + "serverRandom="
        + hex(serverRandom)
        + ", sessionId="
        + hex(sessionId)
        + ", cipherSuites="
        + cipherSuites
        + ", extensions="
        + extensions
        + '}';
  }

  @Override
  public MessageType getType() {
    return TYPE;
  }
}
