package com.protocol7.quincy.tls;

import com.protocol7.quincy.tls.ClientTlsSession.CertificateInvalidException;
import com.protocol7.quincy.tls.ServerTlsSession.ServerHelloAndHandshake;
import com.protocol7.quincy.tls.aead.InitialAEAD;
import com.protocol7.quincy.tls.messages.EncryptedExtensions;
import com.protocol7.quincy.tls.messages.Finished;
import com.protocol7.quincy.tls.messages.ServerCertificate;
import com.protocol7.quincy.tls.messages.ServerCertificateVerify;
import com.protocol7.quincy.utils.Bytes;
import com.protocol7.quincy.utils.Rnd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.security.PrivateKey;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TlsSessionTest {

  private static final String ALPN = "http/0.9";

  private PrivateKey privateKey;
  private final byte[] connectionId = Rnd.rndBytes(16);
  private final ClientTlsSession client =
      new ClientTlsSession(
          InitialAEAD.create(connectionId, true),
          List.of(ALPN),
          TestUtil.tps(),
          NoopCertificateValidator.INSTANCE);
  private ServerTlsSession server;

  @Before
  public void setUp() throws Exception {
    privateKey = KeyUtil.getPrivateKey("src/test/resources/server.der");
    final byte[] serverCert = KeyUtil.getCertFromCrt("src/test/resources/server.crt").getEncoded();

    server =
        new ServerTlsSession(
            InitialAEAD.create(Rnd.rndBytes(4), false),
            List.of(ALPN),
            TestUtil.tps(),
            List.of(serverCert),
            privateKey);
  }

  @Test
  public void handshake() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(Unpooled.wrappedBuffer(shah.getServerHello()));
    final byte[] clientFin = client.handleHandshake(shah.getServerHandshake(), 0).get();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeWithInvalidServerCertVerification() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(Unpooled.wrappedBuffer(shah.getServerHello()));

    final ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    final EncryptedExtensions parsedEE = EncryptedExtensions.parse(bb, true);
    final ServerCertificate parsedSC = ServerCertificate.parse(bb);
    final ServerCertificateVerify parsedSCV = ServerCertificateVerify.parse(bb);
    final Finished parsedSHE = Finished.parse(bb);

    final byte[] sig = parsedSCV.getSignature();

    sig[0]++; // modify signature

    final byte[] scv =
        Bytes.write(
            parsedEE,
            parsedSC,
            new ServerCertificateVerify(parsedSCV.getVerifyType(), sig),
            parsedSHE);

    final byte[] clientFin = client.handleHandshake(scv, 0).get();

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidClientFin() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(Unpooled.wrappedBuffer(shah.getServerHello()));
    final byte[] clientFin = client.handleHandshake(shah.getServerHandshake(), 0).get();

    // modify verification data
    clientFin[clientFin.length - 1]++;

    server.handleClientFinished(clientFin);
  }

  @Test(expected = RuntimeException.class)
  public void handshakeInvalidServerFin() throws CertificateInvalidException {
    final byte[] clientHello = client.startHandshake(connectionId);

    final ServerHelloAndHandshake shah = server.handleClientHello(clientHello);

    client.handleServerHello(Unpooled.wrappedBuffer(shah.getServerHello()));

    final ByteBuf bb = Unpooled.wrappedBuffer(shah.getServerHandshake());
    final EncryptedExtensions parsedEE = EncryptedExtensions.parse(bb, true);
    final ServerCertificate parsedSC = ServerCertificate.parse(bb);
    final ServerCertificateVerify parsedSCV = ServerCertificateVerify.parse(bb);
    final Finished parsedSHE = Finished.parse(bb);

    final byte[] vd = parsedSHE.getVerificationData();

    vd[0]++; // modify verification data

    final byte[] scv = Bytes.write(parsedEE, parsedSC, parsedSCV, new Finished(vd));

    client.handleHandshake(scv, 0).get();
  }
}
