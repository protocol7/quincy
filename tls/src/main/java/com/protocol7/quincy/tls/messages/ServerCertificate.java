package com.protocol7.quincy.tls.messages;

import com.protocol7.quincy.Writeable;
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

public class ServerCertificate implements Writeable {

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
