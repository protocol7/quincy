package com.protocol7.quincy.tls.messages;

import com.protocol7.quincy.Writeable;
import com.protocol7.quincy.tls.extensions.Extension;
import com.protocol7.quincy.utils.Bytes;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerHandshake {

  public static ServerHandshake parse(final ByteBuf bb, final boolean isClient) {

    return new ServerHandshake(
        EncryptedExtensions.parse(bb, isClient),
        ServerCertificate.parse(bb),
        ServerCertificateVerify.parse(bb),
        ServerHandshakeFinished.parse(bb));
  }

  private final EncryptedExtensions encryptedExtensions;
  private final ServerCertificate serverCertificate;
  private final ServerCertificateVerify serverCertificateVerify;
  private final ServerHandshakeFinished serverHandshakeFinished;

  public ServerHandshake(
      final EncryptedExtensions encryptedExtensions,
      final ServerCertificate serverCertificate,
      final ServerCertificateVerify serverCertificateVerify,
      final ServerHandshakeFinished serverHandshakeFinished) {
    this.encryptedExtensions = encryptedExtensions;
    this.serverCertificate = serverCertificate;
    this.serverCertificateVerify = serverCertificateVerify;
    this.serverHandshakeFinished = serverHandshakeFinished;
  }

  public EncryptedExtensions getEncryptedExtensions() {
    return encryptedExtensions;
  }

  public ServerCertificate getServerCertificate() {
    return serverCertificate;
  }

  public ServerCertificateVerify getServerCertificateVerify() {
    return serverCertificateVerify;
  }

  public ServerHandshakeFinished getServerHandshakeFinished() {
    return serverHandshakeFinished;
  }

  public static class EncryptedExtensions implements Writeable {

    public static EncryptedExtensions defaults(final Extension... extensions) {
      return new EncryptedExtensions(List.of(extensions));
    }

    public static EncryptedExtensions parse(final ByteBuf bb, final boolean isClient) {
      // EE
      final int eeType = bb.readByte();
      if (eeType != 0x08) {
        throw new IllegalArgumentException("Invalid EE type: " + eeType);
      }

      final int eeMsgLen = Bytes.read24(bb);
      final int extLen = bb.readShort();

      final ByteBuf ext = bb.readBytes(extLen);
      try {
        final List<Extension> extensions = Extension.parseAll(ext, isClient);

        return new EncryptedExtensions(extensions);
      } finally {
        ext.release();
      }
    }

    private final List<Extension> extensions;

    public EncryptedExtensions(final List<Extension> extensions) {
      this.extensions = extensions;
    }

    public EncryptedExtensions(final Extension... extensions) {
      this.extensions = Arrays.asList(extensions);
    }

    public List<Extension> getExtensions() {
      return extensions;
    }

    public void write(final ByteBuf bb) {
      // EE
      bb.writeByte(0x08);
      final int eeMsgLenPos = bb.writerIndex();
      Bytes.write24(bb, 0);

      final int extLenPos = bb.writerIndex();
      bb.writeShort(0);

      Extension.writeAll(extensions, bb, false);

      Bytes.set24(bb, eeMsgLenPos, bb.writerIndex() - eeMsgLenPos - 3);
      bb.setShort(extLenPos, bb.writerIndex() - extLenPos - 2);
    }
  }

  public static class ServerCertificate implements Writeable {

    public static ServerCertificate parse(final ByteBuf bb) {
      // server cert
      final int serverCertType = bb.readByte();
      if (serverCertType != 0x0b) {
        throw new IllegalArgumentException("Invalid server cert type: " + serverCertType);
      }

      final int scMsgLen = Bytes.read24(bb);
      final int requestContextLen = bb.readByte();

      final byte[] requestContext = new byte[requestContextLen];
      bb.readBytes(requestContext);

      final int certsLen = Bytes.read24(bb);
      final ByteBuf certBB = bb.readBytes(certsLen);
      try {
        final List<byte[]> serverCertificates = new ArrayList<>();

        while (certBB.isReadable()) {
          final int certLen = Bytes.read24(certBB);

          final byte[] cert = new byte[certLen];
          certBB.readBytes(cert);

          serverCertificates.add(cert);

          final int certExtLen = certBB.readShort();
          final byte[] certExt = new byte[certExtLen];
          certBB.readBytes(certExt);
        }

        return new ServerCertificate(requestContext, serverCertificates);
      } finally {
        certBB.release();
      }
    }

    private final byte[] requestContext;
    private final List<byte[]> serverCertificates;

    public ServerCertificate(final byte[] requestContext, final List<byte[]> serverCertificates) {
      this.requestContext = requestContext;
      this.serverCertificates = serverCertificates;
    }

    public ServerCertificate(final byte[]... serverCertificates) {
      this(new byte[0], Arrays.asList(serverCertificates));
    }

    public List<byte[]> getServerCertificates() {
      return serverCertificates;
    }

    public List<Certificate> getAsCertificiates() {
      final List<Certificate> certs = new ArrayList<>();
      try {
        final CertificateFactory f = CertificateFactory.getInstance("X.509");

        for (final byte[] certificate : serverCertificates) {
          final X509Certificate cert =
              (X509Certificate) f.generateCertificate(new ByteArrayInputStream(certificate));
          certs.add(cert);
        }
        return certs;
      } catch (final GeneralSecurityException e) {
        throw new RuntimeException(e);
      }
    }

    public void write(final ByteBuf bb) {
      // server cert
      bb.writeByte(0x0b);
      final int scMsgLenPos = bb.writerIndex();
      Bytes.write24(bb, 0);

      bb.writeByte(requestContext.length);
      bb.writeBytes(requestContext);

      final int certsLenPos = bb.writerIndex();
      Bytes.write24(bb, 0);

      for (final byte[] cert : serverCertificates) {
        Bytes.write24(bb, cert.length);
        bb.writeBytes(cert);

        bb.writeShort(0); // TODO add support for cert extensions
      }

      Bytes.set24(bb, scMsgLenPos, bb.writerIndex() - scMsgLenPos - 3);
      Bytes.set24(bb, certsLenPos, bb.writerIndex() - certsLenPos - 3);
    }
  }

  public static class ServerCertificateVerify implements Writeable {

    public static ServerCertificateVerify parse(final ByteBuf bb) {
      // server cert verify
      final int serverCertVerifyType = bb.readByte();
      if (serverCertVerifyType != 0x0f) {
        throw new IllegalArgumentException(
            "Invalid server cert verify type: " + serverCertVerifyType);
      }

      final int scvMsgLen = Bytes.read24(bb);

      final int signType = bb.readShort();
      final int signLen = bb.readShort();

      final byte[] sign = new byte[signLen];
      bb.readBytes(sign);

      return new ServerCertificateVerify(signType, sign);
    }

    private final int type;
    private final byte[] signature;

    public ServerCertificateVerify(final int type, final byte[] signature) {
      this.type = type;
      this.signature = signature;
    }

    public int getType() {
      return type;
    }

    public byte[] getSignature() {
      return signature;
    }

    public void write(final ByteBuf bb) {
      // server cert verify
      bb.writeByte(0x0f);

      final int scvMsgLenPos = bb.writerIndex();
      Bytes.write24(bb, 0);

      bb.writeShort(type);
      bb.writeShort(signature.length);
      bb.writeBytes(signature);

      Bytes.set24(bb, scvMsgLenPos, bb.writerIndex() - scvMsgLenPos - 3);
    }
  }

  public static class ServerHandshakeFinished implements Writeable {

    public static ServerHandshakeFinished parse(final ByteBuf bb) {
      // server handshake finished
      final int finType = bb.readByte();
      if (finType != 0x14) {
        throw new IllegalArgumentException("Invalid fin type: " + finType);
      }

      final int finLen = Bytes.read24(bb);

      final byte[] verifyData = new byte[finLen];
      bb.readBytes(verifyData);

      return new ServerHandshakeFinished(verifyData);
    }

    private final byte[] verificationData;

    public ServerHandshakeFinished(final byte[] verificationData) {
      this.verificationData = verificationData;
    }

    public byte[] getVerificationData() {
      return verificationData;
    }

    public void write(final ByteBuf bb) {
      // server handshake finished
      bb.writeByte(0x14);
      Bytes.write24(bb, verificationData.length);
      bb.writeBytes(verificationData);
    }
  }
}
