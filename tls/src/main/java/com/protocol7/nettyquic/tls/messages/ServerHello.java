package com.protocol7.nettyquic.tls.messages;

import static com.protocol7.nettyquic.utils.Hex.hex;

import com.google.common.collect.ImmutableList;
import com.protocol7.nettyquic.Writeable;
import com.protocol7.nettyquic.tls.CipherSuite;
import com.protocol7.nettyquic.tls.Group;
import com.protocol7.nettyquic.tls.KeyExchange;
import com.protocol7.nettyquic.tls.extensions.Extension;
import com.protocol7.nettyquic.tls.extensions.ExtensionType;
import com.protocol7.nettyquic.tls.extensions.KeyShare;
import com.protocol7.nettyquic.tls.extensions.SupportedGroups;
import com.protocol7.nettyquic.tls.extensions.SupportedVersions;
import com.protocol7.nettyquic.utils.Bytes;
import com.protocol7.nettyquic.utils.Rnd;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ServerHello implements Writeable {

  private static final byte[] VERSION = new byte[] {0x03, 0x03};

  public static ServerHello defaults(KeyExchange ke, Extension... exts) {
    byte[] clientRandom = Rnd.rndBytes(32);
    byte[] sessionId = new byte[0];
    CipherSuite cipherSuites = CipherSuite.TLS_AES_128_GCM_SHA256;
    List<Extension> extensions =
        ImmutableList.<Extension>builder()
            .add(
                KeyShare.of(ke.getGroup(), ke.getPublicKey()),
                new SupportedGroups(Group.X25519),
                SupportedVersions.TLS13)
            .add(exts)
            .build();

    return new ServerHello(clientRandom, sessionId, cipherSuites, extensions);
  }

  public static ServerHello parse(ByteBuf bb, boolean isClient) {
    int messageType = bb.readByte(); // server hello
    if (messageType != 0x02) {
      throw new IllegalArgumentException("Not a server hello");
    }

    Bytes.read24(bb); // payloadLength

    byte[] version = new byte[2];
    bb.readBytes(version);
    if (!Arrays.equals(version, VERSION)) {
      throw new IllegalArgumentException("Illegal version");
    }

    byte[] serverRandom = new byte[32];
    bb.readBytes(serverRandom); // server random

    int sessionIdLen = bb.readByte();
    byte[] sessionId = new byte[sessionIdLen];

    Optional<CipherSuite> cipherSuite = CipherSuite.parseOne(bb);
    // TODO implement all know cipher suites
    if (!cipherSuite.isPresent()) {
      throw new IllegalArgumentException("Illegal cipher suite");
    }

    bb.readByte(); // compressionMethod

    int extensionLen = bb.readShort();
    ByteBuf extBB = bb.readBytes(extensionLen);
    try {
      List<Extension> extensions = Extension.parseAll(extBB, isClient);
      return new ServerHello(serverRandom, sessionId, cipherSuite.get(), extensions);
    } finally {
      extBB.release();
    }
  }

  public void write(ByteBuf bb) {
    bb.writeByte(0x02);

    int lenPosition = bb.writerIndex();
    // write placeholder
    bb.writeBytes(new byte[3]);
    bb.writeBytes(VERSION);

    bb.writeBytes(serverRandom);
    bb.writeByte(sessionId.length);
    bb.writeBytes(sessionId);
    bb.writeShort(cipherSuites.getValue());
    bb.writeByte(0);

    int extPosition = bb.writerIndex();
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
      byte[] serverRandom, byte[] sessionId, CipherSuite cipherSuites, List<Extension> extensions) {
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

  public Optional<Extension> geExtension(ExtensionType type) {
    for (Extension ext : extensions) {
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
}
